package com.sap.sse.util;


import com.sap.sse.util.SmartFutureCache.UpdateInterval;

public interface ManeuverCache<K, V, U extends UpdateInterval<U>>{

    void resume();

    V get(K key, boolean waitForLatest);

    void suspend();

    void triggerUpdate(K key, U updateInterval);
}