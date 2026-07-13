package com.sap.sailing.gwt.ui.shared.racemap;

import java.util.HashSet;
import java.util.Set;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ScriptElement;

/**
 * The {@link #load(Runnable, String)} method can be used by clients to request the loading of the Google Maps API.
 * The callback passed will be invoked immediately if the API has already been loaded (e.g., by another
 * client call to the {@link #load(Runnable, String)} method within the same frame / document); it will be queued
 * for invocation by a Google Maps API callback function registered otherwise. This callback function
 * is injected at most once when the {@link #load(Runnable, String)} method is invoked for the first time and
 * will trigger all callbacks registered through the {@link #load(Runnable, String)} method until the maps API
 * invokes the callback registered.
 */
public class GoogleMapsLoader {
    /**
     * Note: If you use 3, it will take the newest stable available. We want that, although we didn't test with that yet!
     * Google Release notes: https://developers.google.com/maps/documentation/javascript/releases.
     * Subscribe to https://groups.google.com/forum/#!forum/google-maps-js-api-v3-notify for change notifications.
     */
    public final static String API_VERSION = "3";
    
    /**
     * The required Google Maps libraries; a comma-separated list. See https://developers.google.com/maps/documentation/javascript/libraries
     * for more details. Examples: <tt>drawing,geometry,places,visualization</tt>
     */
    public final static String LIBRARIES = "drawing,geometry";
    
    private static boolean loading = false;
    private static boolean loaded = false;
    private static final Set<Runnable> callbacks = new HashSet<>();
    
    private GoogleMapsLoader() {
    }

    /**
     * @param callback must not be {@code null}.
     */
    public static void load(Runnable callback, String authenticationParams) {
        if (loaded) {
            Scheduler.get().scheduleDeferred(() -> callback.run());
        } else {
            callbacks.add(callback);
            if (!loading) {
                loading = true;
                final boolean mapLibreRequested = isMapLibreRequested();
                setProvider(mapLibreRequested);
                if (mapLibreRequested) {
                    loadMapLibre();
                } else {
                    installCallback();
                    final ScriptElement scriptElement = Document.get().createScriptElement();
                    scriptElement.setSrc("https://maps.googleapis.com/maps/api/js?v="+API_VERSION+"&" + authenticationParams
                            + "&libraries="+LIBRARIES+"&callback=googleMapsLoadedCallback");
                    Document.get().getHead().appendChild(scriptElement);
                }
            }
        }
    }

    public static native boolean isMapLibreRequested() /*-{
        return new $wnd.URLSearchParams($wnd.location.search).get('maps') === 'maplibre';
    }-*/;

    private static native void setProvider(boolean mapLibreRequested) /*-{
        $wnd.__mapsProvider = { provider: mapLibreRequested ? 'maplibre' : 'google', loaded: false };
    }-*/;

    /**
     * Loads MapLibre GL JS plus the Google-Maps-compatible facade from {@code js/maps/}, then fires
     * the queued callbacks via {@link #callback()}. Triggered by {@code ?maps=maplibre} in the page URL.
     */
    private static native void loadMapLibre() /*-{
        var runCallback = $entry(function() {
            @com.sap.sailing.gwt.ui.shared.racemap.GoogleMapsLoader::callback()();
        });
        if ($wnd.maplibregl && $wnd.maplibregl.Map &&
                $wnd.google && $wnd.google.maps && $wnd.google.maps.Map) {
            runCallback();
            return;
        }
        var loadScript = function(src, onload) {
            var s = $doc.createElement('script');
            s.src = src;
            s.onload = onload;
            s.onerror = function() { throw new Error('Failed to load ' + src); };
            $doc.head.appendChild(s);
        };
        var css = $doc.createElement('link');
        css.rel = 'stylesheet';
        css.href = './js/maps/vendor/maplibre-gl/5.9.0/maplibre-gl.css';
        $doc.head.appendChild(css);
        loadScript('./js/maps/vendor/maplibre-gl/5.9.0/maplibre-gl.js', function() {
            var m = $doc.createElement('script');
            m.type = 'module';
            m.text = "import { installGwtMapsCompat } from './js/maps/gwt-maps-maplibre-compat.js?v=race-map-feedback-6'; installGwtMapsCompat(); window.__sailingMapsLoaded();";
            $wnd.__sailingMapsLoaded = runCallback;
            $doc.head.appendChild(m);
        });
    }-*/;
    
    private static void callback() {
        loaded = true;
        markProviderLoaded();
        loading = false;
        callbacks.forEach(Runnable::run);
        callbacks.clear();
        clearCallback();
    }

    private static native void markProviderLoaded() /*-{
        $wnd.__mapsProvider.loaded = true;
    }-*/;
    
    private static native void installCallback() /*-{
        $wnd.googleMapsLoadedCallback = $entry(function() {
            @com.sap.sailing.gwt.ui.shared.racemap.GoogleMapsLoader::callback()();
        });
    }-*/;
    
    private static native void clearCallback() /*-{
        $wnd.googleMapsLoadedCallback = null;
    }-*/;
}
