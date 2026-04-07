package com.sap.sailing.domain.racelogtracking.impl.fixtracker;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.racelog.tracking.SensorFixStore;
import com.sap.sailing.domain.racelogsensortracking.SensorFixMapperFactory;
import com.sap.sailing.domain.tracking.AbstractTrackedRegattaAndRaceObserver;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.TrackedRegattaListener;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sse.replication.FullyInitializedReplicableTracker;
import com.sap.sse.replication.OperationExecutionListener;
import com.sap.sse.replication.OperationWithResult;
import com.sap.sse.replication.OperationWithResultWithIdWrapper;
import com.sap.sse.replication.OperationsToMasterSender;
import com.sap.sse.replication.OperationsToMasterSendingQueue;
import com.sap.sse.replication.ReplicableWithObjectInputStream;
import com.sap.sse.replication.ReplicationMasterDescriptor;
import com.sap.sse.replication.ReplicationService;

/**
 * This is the main entry point of the {@link SensorFixStore} based fix tracking.
 * <p>
 * 
 * This listener is informed about every {@link TrackedRegatta} by {@link RacingEventService} via the implemented
 * {@link TrackedRegattaListener}. For every known {@link TrackedRegatta}, a {@link RegattaLogFixTrackerRaceListener} is
 * started.
 * <p>
 * 
 * In addition this is a {@link ReplicableWithObjectInputStream} because we need to know if the current node is a
 * replica. Replicas must not do any fix tracking because fixes are being loaded on the master and transferred to the
 * replicas through the replication mechanism. That's why in replica state, no {@link RegattaLogFixTrackerRaceListener}
 * instances are created at all.
 */
public class RegattaLogFixTrackerRegattaListener extends AbstractTrackedRegattaAndRaceObserver implements TrackedRegattaListener,
        ReplicableWithObjectInputStream<RegattaLogFixTrackerRegattaListener, OperationWithResult<RegattaLogFixTrackerRegattaListener, ?>> {
    
    private static final Logger log = Logger.getLogger(RegattaLogFixTrackerRegattaListener.class.getName());
    
    private final Map<RegattaAndRaceIdentifier, RaceLogFixTrackerManager> dataTrackers = new ConcurrentHashMap<>();
    private final FullyInitializedReplicableTracker<RacingEventService> racingEventServiceTracker;
    private final SensorFixMapperFactory sensorFixMapperFactory;

    /**
     * This field is expected to be set by the {@link ReplicationService} once it has "adopted" this replicable.
     * The {@link ReplicationService} "injects" this service so it can be used here as a delegate for the
     * {@link OperationsToMasterSendingQueue#scheduleForSending(OperationWithResult, OperationsToMasterSender)}
     * method.
     */
    private OperationsToMasterSendingQueue unsentOperationsToMasterSender;

    public RegattaLogFixTrackerRegattaListener(
            FullyInitializedReplicableTracker<RacingEventService> racingEventServiceTracker,
            SensorFixMapperFactory sensorFixMapperFactory) {
        this.racingEventServiceTracker = racingEventServiceTracker;
        this.sensorFixMapperFactory = sensorFixMapperFactory;
        this.currentlyFillingFromInitialLoad = false;
    }

    @Override
    public synchronized void regattaAdded(TrackedRegatta trackedRegatta) {
        if (!isReplica()) {
            super.regattaAdded(trackedRegatta);
        } else {
            log.warning("Regatta already known, not adding sensor twice");
        }
    }
    
    @Override
    protected void onRaceAdded(RegattaAndRaceIdentifier raceIdentifier, DynamicTrackedRegatta trackedRegatta, DynamicTrackedRace trackedRace) {
        try {
            racingEventServiceTracker.getInitializedService(0).getRaceTrackerByRegattaAndRaceIdentifier(raceIdentifier, (raceTracker) -> {
                try {
                    if (raceTracker != null) {
                        boolean added = raceTracker.add(new RaceTracker.Listener() {
                            @Override
                            public void onTrackerWillStop(boolean preemptive, boolean willBeRemoved) {
                                raceTracker.remove(this);
                                removeRaceLogSensorDataTracker(raceIdentifier, preemptive, willBeRemoved);
                            }
                        });
                        // if !added, the RaceTracker is already stopped, so we are not allowed to start fix tracking
                        if (added) {
                            RaceLogFixTrackerManager trackerManager = new RaceLogFixTrackerManager(
                                    (DynamicTrackedRace) trackedRace, racingEventServiceTracker.getInitializedService(0).getSensorFixStore(),
                                    sensorFixMapperFactory, /* removeOutliersFromCompetitorTracks */ true);
                            RaceLogFixTrackerManager oldInstance = dataTrackers.put(raceIdentifier, trackerManager);
                            if (oldInstance != null) {
                                oldInstance.stop(/* preemptive */ true, /* willBeRemoved */ false);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    protected void onRaceRemoved(DynamicTrackedRace trackedRace) {
        removeRaceLogSensorDataTracker(trackedRace.getRaceIdentifier());
    }
    
    private void removeRaceLogSensorDataTracker(RegattaAndRaceIdentifier raceIdentifier) {
        removeRaceLogSensorDataTracker(raceIdentifier, /* preemptive */ false, /* willBeRemoved */ false);
    }

    private void removeRaceLogSensorDataTracker(RegattaAndRaceIdentifier raceIdentifier, boolean preemptive, boolean willBeRemoved) {
        RaceLogFixTrackerManager currentActiveDataTracker = dataTrackers.get(raceIdentifier);
        if (currentActiveDataTracker != null) {
            currentActiveDataTracker.stop(preemptive, willBeRemoved);
            trackerStopped(raceIdentifier, currentActiveDataTracker);
        }
    }
    
    private void trackerStopped(RegattaAndRaceIdentifier raceIdentifier, RaceLogFixTrackerManager trackerManager) {
        dataTrackers.remove(raceIdentifier, trackerManager);
    }
    
    // Replication related methods and fields
    private final ConcurrentHashMap<OperationExecutionListener<RegattaLogFixTrackerRegattaListener>, OperationExecutionListener<RegattaLogFixTrackerRegattaListener>> operationExecutionListeners = new ConcurrentHashMap<>();
    private volatile boolean currentlyFillingFromInitialLoad;
    private ThreadLocal<Boolean> currentlyApplyingOperationReceivedFromMaster = ThreadLocal.withInitial(() -> false);
    private final Set<OperationWithResultWithIdWrapper<RegattaLogFixTrackerRegattaListener, ?>> operationsSentToMasterForReplication = new HashSet<>();
    private ReplicationMasterDescriptor master;

    @Override
    public Serializable getId() {
        return getClass().getName();
    }

    @Override
    public ReplicationMasterDescriptor getMasterDescriptor() {
        return master;
    }

    @Override
    public void startedReplicatingFrom(ReplicationMasterDescriptor master) {
        this.master = master;
    }

    @Override
    public void stoppedReplicatingFrom(ReplicationMasterDescriptor master) {
        this.master = null;
    }

    @Override
    public Iterable<OperationExecutionListener<RegattaLogFixTrackerRegattaListener>> getOperationExecutionListeners() {
        return operationExecutionListeners.keySet();
    }

    @Override
    public void addOperationExecutionListener(
            OperationExecutionListener<RegattaLogFixTrackerRegattaListener> listener) {
        this.operationExecutionListeners.put(listener, listener);
    }

    @Override
    public void removeOperationExecutionListener(
            OperationExecutionListener<RegattaLogFixTrackerRegattaListener> listener) {
        this.operationExecutionListeners.remove(listener);
    }

    @Override
    public boolean isCurrentlyFillingFromInitialLoad() {
        return currentlyFillingFromInitialLoad;
    }

    @Override
    public void setCurrentlyFillingFromInitialLoad(boolean currentlyFillingFromInitialLoad) {
        this.currentlyFillingFromInitialLoad = currentlyFillingFromInitialLoad;
    }

    @Override
    public boolean isCurrentlyApplyingOperationReceivedFromMaster() {
        return currentlyApplyingOperationReceivedFromMaster.get();
    }

    @Override
    public void setCurrentlyApplyingOperationReceivedFromMaster(boolean currentlyApplyingOperationReceivedFromMaster) {
        this.currentlyApplyingOperationReceivedFromMaster.set(currentlyApplyingOperationReceivedFromMaster);
    }

    @Override
    public void addOperationSentToMasterForReplication(
            OperationWithResultWithIdWrapper<RegattaLogFixTrackerRegattaListener, ?> operationWithResultWithIdWrapper) {
        this.operationsSentToMasterForReplication.add(operationWithResultWithIdWrapper);
    }

    @Override
    public boolean hasSentOperationToMaster(
            OperationWithResult<RegattaLogFixTrackerRegattaListener, ?> operation) {
        return operationsSentToMasterForReplication.remove(operation);
    }

    @Override
    public ObjectInputStream createObjectInputStreamResolvingAgainstCache(InputStream is, Map<String, Class<?>> classLoaderCache) throws IOException {
        return new ObjectInputStream(is);
    }

    @Override
    public synchronized void initiallyFillFromInternal(ObjectInputStream is)
            throws IOException, ClassNotFoundException, InterruptedException {
    }

    @Override
    public void serializeForInitialReplicationInternal(ObjectOutputStream objectOutputStream) throws IOException {
    }

    @Override
    public synchronized void clearReplicaState() throws MalformedURLException, IOException, InterruptedException {
        removeAll();
    }
    
    private boolean isReplica() {
        return master != null;
    }

    @Override
    public void setUnsentOperationToMasterSender(OperationsToMasterSendingQueue service) {
        this.unsentOperationsToMasterSender = service;
    }

    @Override
    public <S, O extends OperationWithResult<S, ?>, T> void scheduleForSending(
            O operationWithResult, OperationsToMasterSender<S, O> sender) {
        if (unsentOperationsToMasterSender != null) {
            unsentOperationsToMasterSender.scheduleForSending(operationWithResult, sender);
        }
    }
}
