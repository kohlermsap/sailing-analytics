package com.sap.sailing.domain.base;

import java.util.ConcurrentModificationException;

import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegattaListener;
import com.sap.sse.common.Util.Pair;

public interface RegattaRegistry {
    /**
     * @return a thread-safe copy of the regattas currently known by the service; it's safe for callers to iterate over
     *         the iterable returned, and no risk of a {@link ConcurrentModificationException} exists
     */
    Iterable<Regatta> getAllRegattas();
    
    Regatta getRegattaByName(String name);
    
    Regatta getRegatta(RegattaIdentifier regattaIdentifier);

    void removeTrackedRegattaListener(TrackedRegattaListener listener);

    void addTrackedRegattaListener(TrackedRegattaListener listener);
}
