package com.sap.sailing.landscape.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.apache.shiro.subject.Subject;

import com.sap.sailing.landscape.SailingAnalyticsMetrics;
import com.sap.sailing.landscape.SailingAnalyticsProcess;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.landscape.Landscape;
import com.sap.sse.landscape.RotatingFileBasedLog;
import com.sap.sse.landscape.aws.AwsApplicationReplicaSet;
import com.sap.sse.landscape.aws.AwsLandscape;
import com.sap.sse.landscape.aws.ReverseProxy;

/**
 * A stateful monitoring task that can be {@link #run run} to observe an {@code ARCHIVE} candidate process and wait for
 * it to be ready for comparing its contents with a production {@code ARCHIVE} instance. When run for the first time,
 * the task check the {@code /gwt/status} end point on {@code candidateHostname} to see whether it is already serving
 * requests. If not, it will re-schedule itself after some delay to check again until either the candidate becomes
 * healthy or a timeout is reached.
 * <p>
 * 
 * If the candidate was seen serving a {@code /gwt/status} request, this task changes state and now looks at the
 * contents of the status response. Four conditions must be fulfilled for the candidate to be considered ready for
 * comparison:
 * 
 * <ol>
 * <li>the overall status must be {@code available: true}.</li>
 * <li>the one-minute system load average must be below 2 (per cent)</li>
 * <li>the default foreground thread pool queue must contain less than 10 tasks</li>
 * <li>the default background thread pool queue must contain less than 10 tasks</li>
 * </ol>
 * 
 * When any of these conditions is not fulfilled, the task will re-schedule itself after some delay to check again until
 * either the candidate fulfills all conditions or a timeout is reached.
 * <p>
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class ArchiveCandidateMonitoringBackgroundTask implements Runnable {
    @FunctionalInterface
    private static interface BooleanSupplierWithException {
        boolean getAsBoolean() throws Exception;
    }
    
    private static final Logger logger = Logger.getLogger(ArchiveCandidateMonitoringBackgroundTask.class.getName());

    private final static Duration DELAY_BETWEEN_CHECKS = Duration.ONE_MINUTE.times(5);
    private final static double MAXIMUM_ONE_MINUTE_SYSTEM_LOAD_AVERAGE = 2.0;
    private final static int MAXIMUM_THREAD_POOL_QUEUE_SIZE = 10;
    private final static Optional<Duration> TIMEOUT_FIRST_CONTACT = Optional.of(Landscape.WAIT_FOR_PROCESS_TIMEOUT.get().plus(Landscape.WAIT_FOR_HOST_TIMEOUT.get()));
    private final Subject subject;
    private final AwsLandscape<String> landscape;
    private final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet;
    private final ReverseProxy<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>, RotatingFileBasedLog> reverseProxyCluster;
    private final String optionalKeyName;
    private final byte[] privateKeyEncryptionPassphrase;
    private final ScheduledExecutorService executor;
    private final TimePoint firstRun;
    private final List<String> messagesToSendToProcessOwner;
    private Iterable<BooleanSupplierWithException> checks;
    private Iterator<BooleanSupplierWithException> checksIterator;
    private BooleanSupplierWithException currentCheck;
    private boolean candidateSeenServingStatusRequest;
    
    public ArchiveCandidateMonitoringBackgroundTask(Subject subject, AwsLandscape<String> landscape,
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            String candidateHostname,
            ReverseProxy<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>, RotatingFileBasedLog> reverseProxyCluster,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase, ScheduledExecutorService executor) {
        this.subject = subject;
        this.landscape = landscape;
        this.replicaSet = replicaSet;
        this.reverseProxyCluster = reverseProxyCluster;
        this.optionalKeyName = optionalKeyName;
        this.privateKeyEncryptionPassphrase = privateKeyEncryptionPassphrase;
        this.executor = executor;
        this.firstRun = TimePoint.now();
        this.messagesToSendToProcessOwner = new LinkedList<>();
        this.candidateSeenServingStatusRequest = false;
        this.checks = Arrays.asList(
                this::isReady,
                this::hasLowEnoughSystemLoad,
                this::hasShortEnoughDefaultBackgroundThreadPoolExecutorQueue,
                this::hasShortEnoughDefaultForegroundThreadPoolExecutorQueue,
                this::compareServersWithRestAPI,
                this::compareServersByLeaderboardGroups);
        this.checksIterator = this.checks.iterator();
        this.currentCheck = checksIterator.next();
    }

    @Override
    public void run() {
        try {
            if (currentCheck.getAsBoolean()) {
                logger.info("Another check passed.");
                // the check passed; proceed to next check, if any
                currentCheck = checksIterator.hasNext() ? checksIterator.next() : null;
            }
            if (currentCheck != null) {
                logger.info("More checks to do; re-scheduling.");
                // re-schedule this task to run next check in a while
                executor.schedule(this, DELAY_BETWEEN_CHECKS.asMillis(), TimeUnit.MILLISECONDS);
            } else {
                logger.info("Done with all checks; candidate is ready for comparison.");
                // all checks passed; candidate is ready for comparison; nothing more to do here
            }
        } catch (Exception e) {
            logger.warning("Exception while running check " + currentCheck + " for candidate " + replicaSet.getMaster().getHost().getHostname() + ": " + e.getMessage());
        }
        
    }
        
    private Boolean isReady() throws IOException {
        return replicaSet.getMaster().isReady(Landscape.WAIT_FOR_PROCESS_TIMEOUT);
    }
    
    private boolean hasLowEnoughSystemLoad() throws TimeoutException, Exception {
        return replicaSet.getMaster().getLastMinuteSystemLoadAverage(Landscape.WAIT_FOR_PROCESS_TIMEOUT) < MAXIMUM_ONE_MINUTE_SYSTEM_LOAD_AVERAGE;
    }
    
    private boolean hasShortEnoughDefaultBackgroundThreadPoolExecutorQueue() throws TimeoutException, Exception {
        return replicaSet.getMaster().getDefaultBackgroundThreadPoolExecutorQueueSize(Landscape.WAIT_FOR_PROCESS_TIMEOUT) < MAXIMUM_THREAD_POOL_QUEUE_SIZE;
    }

    private boolean hasShortEnoughDefaultForegroundThreadPoolExecutorQueue() throws TimeoutException, Exception {
        return replicaSet.getMaster().getDefaultForegroundThreadPoolExecutorQueueSize(Landscape.WAIT_FOR_PROCESS_TIMEOUT) < MAXIMUM_THREAD_POOL_QUEUE_SIZE;
    }
    
    private boolean compareServersWithRestAPI() throws Exception {
        // TODO
        return false;
    }
    
    private boolean compareServersByLeaderboardGroups() throws Exception {
        // TODO
        return false;
    }
}
