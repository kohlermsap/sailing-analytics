package com.sap.sailing.awssensorfixstore.impl;

import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.racelog.tracking.FixReceivedListener;
import com.sap.sailing.domain.racelog.tracking.SensorFixStore;
import com.sap.sse.common.Duration;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Timed;
import com.sap.sse.common.TransformationException;
import com.sap.sse.common.Util.Triple;

public class AwsSensorFixStore implements SensorFixStore {

    @Override
    public <FixT extends Timed> void loadFixes(Consumer<FixT> consumer, DeviceIdentifier deviceIdentifier,
            TimePoint start, TimePoint end, boolean toIsInclusive)
            throws NoCorrespondingServiceRegisteredException, TransformationException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public <FixT extends Timed> void loadFixes(Consumer<FixT> consumer, DeviceIdentifier deviceIdentifier,
            TimePoint start, TimePoint end, boolean inclusive, BooleanSupplier isPreemptiveStopped,
            Consumer<Double> progressReporter)
            throws NoCorrespondingServiceRegisteredException, TransformationException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public <FixT extends Timed> void storeFix(DeviceIdentifier device, FixT fix) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public <FixT extends Timed> Iterable<Triple<RegattaAndRaceIdentifier, Boolean, Duration>> storeFixes(
            DeviceIdentifier device, Iterable<FixT> fixes, boolean returnManeuverUpdate, boolean returnLiveDelay) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addListener(FixReceivedListener<? extends Timed> listener, DeviceIdentifier device) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeListener(FixReceivedListener<? extends Timed> listener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeListener(FixReceivedListener<? extends Timed> listener, DeviceIdentifier device) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public TimeRange getTimeRangeCoveredByFixes(DeviceIdentifier device)
            throws TransformationException, NoCorrespondingServiceRegisteredException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getNumberOfFixes(DeviceIdentifier device)
            throws TransformationException, NoCorrespondingServiceRegisteredException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public <FixT extends Timed> Map<DeviceIdentifier, FixT> getFixLastReceived(Iterable<DeviceIdentifier> forDevices)
            throws TransformationException, NoCorrespondingServiceRegisteredException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <FixT extends Timed> boolean loadOldestFix(Consumer<FixT> consumer, DeviceIdentifier device,
            TimeRange timeRangetoLoad) throws NoCorrespondingServiceRegisteredException, TransformationException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public <FixT extends Timed> boolean loadYoungestFix(Consumer<FixT> consumer, DeviceIdentifier device,
            TimeRange timeRangetoLoad) throws NoCorrespondingServiceRegisteredException, TransformationException {
        // TODO Auto-generated method stub
        return false;
    }

}
