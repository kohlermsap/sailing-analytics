package com.sap.sailing.domain.igtimiadapter.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.igtimi.IgtimiDevice.DeviceManagement;
import com.igtimi.IgtimiStream.Msg;

public class RiotServerReplicationTest extends AbstractRiotServerReplicationTest {
    @BeforeEach
    public void clear() {
        master.clear();
        replica.clear();
    }
    
    @Test
    public void simpleDeviceCreationTest() throws IllegalAccessException, InterruptedException {
        final String deviceSerialNumber = "12345";
        assertNull(replica.getDeviceBySerialNumber(deviceSerialNumber));
        master.createDevice(deviceSerialNumber);
        replicaReplicator.waitUntilQueueIsEmpty();
        Thread.sleep(1000); // wait for replica to receive and apply after replicator has emptied its queue
        assertNotNull(replica.getDeviceBySerialNumber(deviceSerialNumber));
    }
    
    @Test
    public void triggerDeviceCreationWithMessagesTest() throws IllegalAccessException, InterruptedException {
        final String deviceSerialNumber = "12345";
        final Msg message = Msg.newBuilder().setDeviceManagement(DeviceManagement.newBuilder().setSerialNumber(deviceSerialNumber)).build();
        assertNull(master.getDeviceBySerialNumber(deviceSerialNumber));
        assertNull(replica.getDeviceBySerialNumber(deviceSerialNumber));
        replica.internalNotifyListeners(message, deviceSerialNumber); // assuming that this will lead to device creation on both ends, and buffering
        replicaReplicator.waitUntilQueueIsEmpty();
        Thread.sleep(3000); // wait for replica to receive and apply after replicator has emptied its queue
        assertNotNull(master.getDeviceBySerialNumber(deviceSerialNumber));
        assertNotNull(replica.getDeviceBySerialNumber(deviceSerialNumber));
    }
}
