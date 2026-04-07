package com.sap.sailing.domain.racelog.tracking;

import java.util.Collections;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sse.common.Duration;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Timed;
import com.sap.sse.common.TransformationException;
import com.sap.sse.common.Util.Triple;

public enum EmptySensorFixStore implements SensorFixStore {
    INSTANCE;

    @Override
    public void addListener(FixReceivedListener<? extends Timed> listener, DeviceIdentifier device) {
    }

    @Override
    public void removeListener(FixReceivedListener<? extends Timed> listener) {
    }

    @Override
    public void removeListener(FixReceivedListener<? extends Timed> listener, DeviceIdentifier device) {
    }

    @Override
    public TimeRange getTimeRangeCoveredByFixes(DeviceIdentifier device) {
        return null;
    }

    @Override
    public long getNumberOfFixes(DeviceIdentifier device) {
        return 0;
    }

    @Override
    public <FixT extends Timed> void loadFixes(Consumer<FixT> consumer, DeviceIdentifier deviceIdentifier,
            TimePoint start, TimePoint end, boolean inclusive) throws NoCorrespondingServiceRegisteredException,
            TransformationException {
    }

    @Override
    public <FixT extends Timed> void loadFixes(Consumer<FixT> consumer, DeviceIdentifier deviceIdentifier,
            TimePoint start, TimePoint end, boolean inclusive, BooleanSupplier isPreemptiveStopped,
            Consumer<Double> progressReporter)
            throws NoCorrespondingServiceRegisteredException, TransformationException {
    }

    @Override
    public <FixT extends Timed> void storeFix(DeviceIdentifier device, FixT fix) {
    }

    @Override
    public <FixT extends Timed> Iterable<Triple<RegattaAndRaceIdentifier, Boolean, Duration>> storeFixes(DeviceIdentifier device,
            Iterable<FixT> fixes, boolean returnManeuverUpdate, boolean returnLiveDelay) {
        return Collections.emptySet();
    }

    @Override
    public <FixT extends Timed> Map<DeviceIdentifier, FixT> getFixLastReceived(Iterable<DeviceIdentifier> forDevices) {
        return null;
    }

    @Override
    public <FixT extends Timed> boolean loadOldestFix(Consumer<FixT> consumer, DeviceIdentifier device,
            TimeRange timeRangetoLoad) throws NoCorrespondingServiceRegisteredException, TransformationException {
        return false;
    }

    @Override
    public <FixT extends Timed> boolean loadYoungestFix(Consumer<FixT> consumer, DeviceIdentifier device,
            TimeRange timeRangetoLoad) throws NoCorrespondingServiceRegisteredException, TransformationException {
        return false;
    }
}
