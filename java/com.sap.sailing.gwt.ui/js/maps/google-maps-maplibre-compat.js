// Core facade: implements the Google Maps JavaScript API surface on MapLibre GL JS.
// Keep GWT wrapper conventions in gwt-maps-maplibre-compat.js.
import { applyRaceStyle, createArrowSvg, createRaceStyle, lngLat, setSatelliteVisible } from './maplibre-test-utils.js?v=race-map-feedback-8';

function asLngLatLiteral(value) {
    if (Array.isArray(value)) return { lat: value[1], lng: value[0] };
    if (typeof value.lat === 'function') return { lat: value.lat(), lng: value.lng() };
    return value;
}

const GOOGLE_ZOOM_SCALE_CORRECTION = Math.log2(1.00225);

function toMapLibreZoom(googleZoom) {
    return googleZoom - 1 + GOOGLE_ZOOM_SCALE_CORRECTION;
}

function toGoogleZoom(mapLibreZoom) {
    return mapLibreZoom + 1 - GOOGLE_ZOOM_SCALE_CORRECTION;
}

function isSatelliteMapType(mapTypeId) {
    const id = typeof mapTypeId === 'string' ? mapTypeId.toLowerCase() : mapTypeId;
    return id === 'satellite' || id === 'hybrid';
}

function circleCoordinates(center, radiusMeters, steps = 64) {
    const { lat, lng } = asLngLatLiteral(center);
    const earthRadius = 6371000;
    const distance = radiusMeters / earthRadius;
    const latRad = lat * Math.PI / 180;
    const lngRad = lng * Math.PI / 180;
    const coords = [];
    for (let i = 0; i <= steps; i++) {
        const bearing = i / steps * Math.PI * 2;
        const pointLat = Math.asin(Math.sin(latRad) * Math.cos(distance) + Math.cos(latRad) * Math.sin(distance) * Math.cos(bearing));
        const pointLng = lngRad + Math.atan2(Math.sin(bearing) * Math.sin(distance) * Math.cos(latRad), Math.cos(distance) - Math.sin(latRad) * Math.sin(pointLat));
        coords.push([pointLng * 180 / Math.PI, pointLat * 180 / Math.PI]);
    }
    return coords;
}

class CompatLatLng {
    constructor(lat, lng) {
        this._lat = lat;
        this._lng = lng;
    }
    lat() { return this._lat; }
    lng() { return this._lng; }
    getLatitude() { return this._lat; }
    getLongitude() { return this._lng; }
    getLatDeg() { return this._lat; }
    getLngDeg() { return this._lng; }
    getX() { return this._lng; }
    getY() { return this._lat; }
    equals(other) { const point = asLngLatLiteral(other); return this._lat === point.lat && this._lng === point.lng; }
    add(other) { const point = asLngLatLiteral(other); return new CompatLatLng(this._lat + point.lat, this._lng + point.lng); }
    dotProduct(other) { const point = asLngLatLiteral(other); return this._lng * point.lng + this._lat * point.lat; }
    getNorm() { return Math.hypot(this._lng, this._lat); }
    getBearingGreatCircle(other) {
        const point = asLngLatLiteral(other);
        const toRad = deg => deg * Math.PI / 180;
        const y = Math.sin(toRad(point.lng - this._lng)) * Math.cos(toRad(point.lat));
        const x = Math.cos(toRad(this._lat)) * Math.sin(toRad(point.lat)) - Math.sin(toRad(this._lat)) * Math.cos(toRad(point.lat)) * Math.cos(toRad(point.lng - this._lng));
        return (Math.atan2(y, x) * 180 / Math.PI + 360) % 360;
    }
    getAsSignedDecimalDegrees() { return `${this._lat.toFixed(5)}, ${this._lng.toFixed(5)}`; }
    getAsDegreesAndDecimalMinutesWithCardinalPoints() { return `${Math.abs(this._lat).toFixed(5)}°${this._lat >= 0 ? 'N' : 'S'} ${Math.abs(this._lng).toFixed(5)}°${this._lng >= 0 ? 'E' : 'W'}`; }
    toJSON() { return { lat: this._lat, lng: this._lng }; }
}

class CompatPoint {
    constructor(x, y) {
        this.x = x;
        this.y = y;
    }
    getX() { return this.x; }
    getY() { return this.y; }
    getLatDeg() { return this.y; }
    getLngDeg() { return this.x; }
    equals(other) { return this.x === other.x && this.y === other.y; }
}

class CompatLatLngBounds {
    constructor(sw, ne) {
        this.sw = asLngLatLiteral(sw);
        this.ne = asLngLatLiteral(ne);
    }
    toMapLibreBounds() {
        return [[this.sw.lng, this.sw.lat], [this.ne.lng, this.ne.lat]];
    }
    contains(latLng) {
        const point = asLngLatLiteral(latLng);
        return point.lat >= this.sw.lat && point.lat <= this.ne.lat && point.lng >= this.sw.lng && point.lng <= this.ne.lng;
    }
    extend(latLng) {
        const point = asLngLatLiteral(latLng);
        this.sw = { lat: Math.min(this.sw.lat, point.lat), lng: Math.min(this.sw.lng, point.lng) };
        this.ne = { lat: Math.max(this.ne.lat, point.lat), lng: Math.max(this.ne.lng, point.lng) };
        return this;
    }
    getNorthEast() { return new CompatLatLng(this.ne.lat, this.ne.lng); }
    getSouthWest() { return new CompatLatLng(this.sw.lat, this.sw.lng); }
    equals(other) {
        const bounds = other instanceof CompatLatLngBounds ? other : new CompatLatLngBounds(other.getSouthWest(), other.getNorthEast());
        return this.sw.lat === bounds.sw.lat && this.sw.lng === bounds.sw.lng && this.ne.lat === bounds.ne.lat && this.ne.lng === bounds.ne.lng;
    }
    intersects(other) {
        const bounds = other instanceof CompatLatLngBounds ? other : new CompatLatLngBounds(other.getSouthWest(), other.getNorthEast());
        return !(bounds.sw.lng > this.ne.lng || bounds.ne.lng < this.sw.lng || bounds.sw.lat > this.ne.lat || bounds.ne.lat < this.sw.lat);
    }
}

class CompatMap {
    constructor(element, options = {}) {
        this.element = element;
        this.options = options;
        this.listeners = new Map();
        this.mapTypes = new Map();
        this.loaded = false;
        this.deferred = [];
        this.detachedPolylineBackings = [];
        this.polylineBackings = new Map();
        this.polylineOwners = new Map();
        this.polylinePublishedIds = new Set();
        this.dirtyPolylineIds = new Set();
        this.removedPolylineIds = new Set();
        this.polylineFlushFrame = null;
        this.nextPolylineSequence = 1;
        this.cameraChangedSinceIdle = true;
        element.style.position = element.style.position || 'relative';
        element.style.isolation = 'isolate';
        this.overlayLayer = document.createElement('div');
        this.markerLayer = document.createElement('div');
        this.overlayMouseTarget = document.createElement('div');
        this.floatPane = document.createElement('div');
        Object.assign(this.overlayLayer.style, { position: 'absolute', inset: '0', zIndex: 5, pointerEvents: 'none', transformOrigin: '50% 50%' });
        Object.assign(this.markerLayer.style, { position: 'absolute', inset: '0', zIndex: 7, pointerEvents: 'none' });
        Object.assign(this.overlayMouseTarget.style, { position: 'absolute', inset: '0', zIndex: 10, pointerEvents: 'none', transformOrigin: '50% 50%' });
        Object.assign(this.floatPane.style, { position: 'absolute', inset: '0', zIndex: 15, pointerEvents: 'none' });
        element.append(this.overlayLayer, this.markerLayer, this.overlayMouseTarget, this.floatPane);
        for (const pane of [this.markerLayer, this.overlayMouseTarget, this.floatPane]) {
            for (const name of ['click', 'dblclick', 'contextmenu']) pane.addEventListener(name, event => event.stopPropagation());
        }
        this.updateOverlayPointerEvents = () => {
            const width = element.clientWidth;
            const height = element.clientHeight;
            for (const canvas of this.overlayMouseTarget.querySelectorAll('canvas')) {
                const fullMapCanvas = width > 0 && height > 0 && canvas.width >= width && canvas.height >= height;
                const pointerEvents = canvas.style.cursor === 'pointer' && !fullMapCanvas ? 'auto' : 'none';
                if (canvas.style.pointerEvents !== pointerEvents) canvas.style.pointerEvents = pointerEvents;
            }
        };
        this.overlayPointerObserver = new MutationObserver(this.updateOverlayPointerEvents);
        this.overlayPointerObserver.observe(this.overlayMouseTarget, { childList: true, subtree: true, attributes: true, attributeFilter: ['width', 'height'] });
        const center = asLngLatLiteral(options.center || { lat: 0, lng: 0 });
        const initialCenter = Number.isFinite(center.lat) && Number.isFinite(center.lng) ? center : { lat: 0, lng: 0 };
        this.map = new maplibregl.Map({
            container: element,
            style: createRaceStyle(),
            center: lngLat(initialCenter),
            zoom: toMapLibreZoom(options.zoom ?? 0),
            bearing: options.heading || 0,
            pitch: 0
        });
        // MapLibre listens for gestures on this container, so interactive panes must be descendants.
        this.map.getCanvasContainer().append(this.overlayLayer, this.markerLayer, this.overlayMouseTarget, this.floatPane);
        this.resizeObserver = new ResizeObserver(() => { this.map.resize(); this.updateOverlayPointerEvents(); });
        this.resizeObserver.observe(element);
        const controlPositions = {
            1: ['10px', '', '', '10px'], 2: ['10px', '', '', '50%'], 3: ['10px', '10px', '', ''],
            // RaceMap's LEFT_TOP control already supplies its own 10px margin; keep overlays and boats untouched.
            4: ['50%', '', '', '10px'], 5: ['60px', '', '', '0px'], 6: ['', '', '23px', '0px'],
            7: ['60px', '10px', '', ''], 8: ['50%', '10px', '', ''], 9: ['', '0px', '10px', ''],
            10: ['', '', '10px', '10px'], 11: ['', '', '10px', '50%'], 12: ['', '10px', '10px', '']
        };
        this.controls = Array.from({ length: 13 }, (_, position) => {
            const container = document.createElement('div');
            const [top, right, bottom, left] = controlPositions[position] || ['', '', '', ''];
            Object.assign(container.style, { position: 'absolute', top, right, bottom, left, zIndex: 20, pointerEvents: 'auto' });
            // Match Google control metrics; MapLibre's 12px/20px font adds baseline space below inline canvases.
            if (position === 5) container.style.font = '400 11px Roboto, Arial, sans-serif';
            if (position === 2 || position === 11) container.style.transform = 'translateX(-50%)';
            if (position === 4 || position === 8) container.style.transform = 'translateY(-50%)';
            element.appendChild(container);
            const items = [];
            return {
                push: child => { items.push(child); container.appendChild(child); return items.length; },
                pop: () => { const child = items.pop(); child?.remove(); return child; },
                getAt: index => items[index],
                getLength: () => items.length,
                insertAt: (index, child) => { items.splice(index, 0, child); container.insertBefore(child, container.children[index] || null); },
                removeAt: index => { const [child] = items.splice(index, 1); child?.remove(); return child; },
                setAt: (index, child) => { const old = items[index]; items[index] = child; old?.replaceWith(child); }
            };
        });
        if (typeof this.map._getUIString !== 'function') this.map._getUIString = key => key;
        if (options.zoomControl !== false) {
            this.map.addControl(new maplibregl.NavigationControl({ visualizePitch: false }), 'top-right');
        }
        applyRaceStyle(this.map, options.seaMarksVisible);
        setSatelliteVisible(this.map, isSatelliteMapType(options.mapTypeId));
        this.map.on('move', () => {
            this.cameraChangedSinceIdle = true;
            this.emit('bounds_changed');
            this.emit('center_changed');
        });
        this.map.on('zoomstart', event => {
            this.cameraChangedSinceIdle = true;
            this.userZoomInProgress = !!event.originalEvent;
            this.emit('zoom_changed');
            if (this.userZoomInProgress) this.emit('dragend');
        });
        this.map.on('rotate', () => this.emit('heading_changed'));
        this.map.on('idle', () => {
            if (!this.cameraChangedSinceIdle) return;
            this.cameraChangedSinceIdle = false;
            const emitIdle = () => {
                this.userZoomInProgress = false;
                this.emit('bounds_changed');
                this.emit('idle');
            };
            if (this.userZoomInProgress) {
                clearTimeout(this.userZoomIdleTimer);
                this.userZoomIdleTimer = setTimeout(emitIdle, 550);
            }
            else emitIdle();
        });
        this.map.on('dragend', () => this.emit('dragend'));
        for (const name of ['click', 'mousedown', 'mousemove', 'mouseout', 'mouseup', 'dblclick']) {
            this.map.on(name, event => {
                const mapEvent = { latLng: new CompatLatLng(event.lngLat.lat, event.lngLat.lng) };
                if (name === 'mousemove') {
                    clearTimeout(this.mouseMoveTimer);
                    this.mouseMoveTimer = setTimeout(() => {
                        // ponytail: layer-filtered queryRenderedFeatures avoids scanning the full style (100+ base-tile layers).
                        // Upgrade path if still hot: rAF-coalesce, or merge all compat polylines into one shared source.
                        const hitLayers = this.getCompatHitLayerIds();
                        const overPolyline = event.point && hitLayers.length > 0 &&
                            this.map.queryRenderedFeatures?.(event.point, { layers: hitLayers }).length > 0;
                        if (!event.__compatOverlayHandled && !overPolyline) this.emit(name, mapEvent);
                    });
                }
                else this.emit(name, mapEvent);
                // ponytail: mousedown avoids MapLibre dragstart re-entry; upgrade if non-drag clicks matter.
                if (name === 'mousedown') queueMicrotask(() => this.emit('dragstart', mapEvent));
            });
        }
        this.map.on('contextmenu', event => this.emit('rightclick', { latLng: new CompatLatLng(event.lngLat.lat, event.lngLat.lng) }));
        this.map.on('load', () => {
            this.initializePolylineBatch();
            this.loaded = true;
            this.deferred.splice(0).forEach(action => action());
        });
    }
    initializePolylineBatch() {
        if (this.map.getSource('compat-polylines')) return;
        this.map.addSource('compat-polylines', {
            type: 'geojson',
            data: { type: 'FeatureCollection', features: [] }
        });
        const sort = ['get', 'sortKey'];
        this.map.addLayer({
            id: 'compat-polylines',
            type: 'line',
            source: 'compat-polylines',
            layout: { 'line-sort-key': sort },
            paint: {
                'line-color': ['get', 'color'],
                'line-opacity': ['case', ['get', 'visible'], ['get', 'opacity'], 0],
                'line-width': ['get', 'width']
            }
        });
        this.map.addLayer({
            id: 'compat-polylines-hit',
            type: 'line',
            source: 'compat-polylines',
            filter: ['all', ['==', ['get', 'visible'], true], ['==', ['get', 'interactive'], true]],
            layout: { 'line-sort-key': sort },
            paint: {
                'line-color': '#000000',
                'line-opacity': 0,
                'line-width': ['max', ['get', 'width'], 8]
            }
        });
    }
    registerPolyline(polyline) {
        if (!polyline.sequence) polyline.sequence = this.nextPolylineSequence++;
        this.polylineOwners.set(polyline.featureId, polyline);
        this.removedPolylineIds.delete(polyline.featureId);
        this.dirtyPolylineIds.add(polyline.featureId);
        this.schedulePolylineFlush();
    }
    markPolylineDirty(polyline) {
        if (this.polylineOwners.get(polyline.featureId) !== polyline) return;
        this.dirtyPolylineIds.add(polyline.featureId);
        this.schedulePolylineFlush();
    }
    unregisterPolyline(polyline) {
        if (this.polylineOwners.get(polyline.featureId) !== polyline) return;
        this.polylineOwners.delete(polyline.featureId);
        this.dirtyPolylineIds.delete(polyline.featureId);
        this.removedPolylineIds.add(polyline.featureId);
        this.schedulePolylineFlush();
    }
    schedulePolylineFlush() {
        if (this.polylineFlushFrame !== null) return;
        this.polylineFlushFrame = requestAnimationFrame(() => {
            this.polylineFlushFrame = null;
            this.flushPolylineBatch();
        });
    }
    flushPolylineBatch() {
        const source = this.map.getSource('compat-polylines');
        if (!source) return;
        const add = [];
        const update = [];
        for (const id of this.dirtyPolylineIds) {
            const owner = this.polylineOwners.get(id);
            if (!owner) continue;
            const feature = owner.feature();
            if (this.polylinePublishedIds.has(id)) {
                update.push({
                    id,
                    newGeometry: feature.geometry,
                    addOrUpdateProperties: Object.entries(feature.properties).map(([key, value]) => ({ key, value }))
                });
            } else {
                add.push(feature);
                this.polylinePublishedIds.add(id);
            }
        }
        const remove = [...this.removedPolylineIds].filter(id => this.polylinePublishedIds.delete(id));
        this.dirtyPolylineIds.clear();
        this.removedPolylineIds.clear();
        if (add.length || update.length || remove.length) source.updateData({ add, update, remove });
    }
    addListener(name, handler) {
        if (!this.listeners.has(name)) this.listeners.set(name, new Set());
        this.listeners.get(name).add(handler);
        return { remove: () => this.listeners.get(name)?.delete(handler) };
    }
    emit(name, event = {}) {
        for (const handler of this.listeners.get(name) || []) handler(event);
    }
    getCompatHitLayerIds() {
        const ids = [];
        for (const backing of this.polylineBackings.values()) {
            if (this.map.getLayer(backing.hitId)) ids.push(backing.hitId);
        }
        return ids;
    }
    ready(action) {
        if (this.loaded) action();
        else this.deferred.push(action);
    }
    getDiv() { return this.element; }
    getBounds() {
        const bounds = this.map.getBounds();
        return new CompatLatLngBounds(
            { lat: bounds.getSouth(), lng: bounds.getWest() },
            { lat: bounds.getNorth(), lng: bounds.getEast() }
        );
    }
    getZoom() { return toGoogleZoom(this.map.getZoom()); }
    setZoom(zoom) {
        const options = { zoom: toMapLibreZoom(zoom) };
        if (this.pendingPanCenter) {
            options.center = this.pendingPanCenter;
            this.pendingPanCenter = null;
        }
        this.map.jumpTo(options);
    }
    getCenter() {
        const center = this.map.getCenter();
        return new CompatLatLng(center.lat, center.lng);
    }
    setCenter(center) { this.map.setCenter(lngLat(asLngLatLiteral(center))); }
    fitBounds(bounds) { this.map.fitBounds(bounds.toMapLibreBounds()); }
    panTo(position) {
        const center = lngLat(asLngLatLiteral(position));
        this.pendingPanCenter = center;
        this.map.once('moveend', () => {
            if (this.pendingPanCenter !== center) return;
            const current = this.map.getCenter();
            if (Math.abs(current.lng - center[0]) < 1e-9 && Math.abs(current.lat - center[1]) < 1e-9) this.pendingPanCenter = null;
        });
        this.map.panTo(center);
    }
    setHeading(degrees) { this.map.rotateTo(degrees, { duration: 500, easing: t => t * (2 - t) }); }
    getHeading() { return (this.map.getBearing() + 360) % 360; }
    setMapTypeId(mapTypeId) { this.options.mapTypeId = mapTypeId; setSatelliteVisible(this.map, isSatelliteMapType(mapTypeId)); }
    getMapTypeId() { return this.options.mapTypeId || 'roadmap'; }
    setOptions(options = {}) {
        Object.assign(this.options, options);
        const camera = {};
        if ('center' in options) camera.center = lngLat(asLngLatLiteral(options.center));
        if ('zoom' in options) camera.zoom = toMapLibreZoom(options.zoom);
        if ('heading' in options) camera.bearing = options.heading;
        if (Object.keys(camera).length) this.map.jumpTo(camera);
        if ('seaMarksVisible' in options) applyRaceStyle(this.map, options.seaMarksVisible);
        if ('mapTypeId' in options) setSatelliteVisible(this.map, isSatelliteMapType(options.mapTypeId));
    }
    resize() { this.map.resize(); this.emit('resize'); }
}

let nextOverlayId = 1;
const POLYLINE_PATH_DEBOUNCE_MS = globalThis.__maplibreCompatPathDebounceMs ?? 25;
const POLYLINE_BACKING_TTL_MS = globalThis.__maplibreCompatBackingTtlMs ?? 10000;
const POLYLINE_EVENTS = [
    ['mouseenter', 'mouseover'], ['mousemove', 'mousemove'], ['mouseleave', 'mouseout'],
    ['mousedown', 'mousedown'], ['mouseup', 'mouseup'], ['click', 'click']
];

function createCompatArray(values = []) {
    if (values?.__compatArrayValues && values?.__compatArraySubscribers) return values;
    const items = values?.toArray ? values.toArray() : [...values];
    const subscribers = new Set();
    const notify = () => {
        for (const subscriber of [...subscribers]) subscriber();
    };
    return {
        __compatArrayValues: items,
        __compatArraySubscribers: subscribers,
        getAt: index => items[index],
        get: index => items[index],
        getLength: () => items.length,
        push: value => { const length = items.push(value); notify(); return length; },
        pop: () => { const value = items.pop(); notify(); return value; },
        insertAt: (index, value) => { items.splice(index, 0, value); notify(); },
        removeAt: index => { const value = items.splice(index, 1)[0]; notify(); return value; },
        setAt: (index, value) => { items[index] = value; notify(); },
        add: value => { const length = items.push(value); notify(); return length; },
        clear: () => { items.splice(0); notify(); },
        forEach: callback => items.forEach((value, index) => callback(value, index)),
        getArray: () => items,
        toArray: () => [...items]
    };
}

class CompatPolyline {
    constructor(options = {}) {
        this.options = options;
        this.path = createCompatArray(options.path || []);
        this.pathChanged = () => this.schedulePublish();
        this.path.__compatArraySubscribers.add(this.pathChanged);
        this.visible = options.visible !== false;
        this.listeners = new Map();
        this.id = `compat-polyline-${nextOverlayId++}`;
        if (options.map) this.setMap(options.map);
    }
    addListener(name, handler) {
        if (!this.listeners.has(name)) this.listeners.set(name, new Set());
        this.listeners.get(name).add(handler);
        return { remove: () => this.listeners.get(name)?.delete(handler) };
    }
    emit(name, event) {
        for (const handler of this.listeners.get(name) || []) handler(event);
    }
    feature() {
        return {
            type: 'Feature',
            properties: { color: this.options.strokeColor || '#000000', opacity: this.options.strokeOpacity ?? 1, width: this.options.strokeWeight || 1 },
            geometry: { type: 'LineString', coordinates: this.path.__compatArrayValues.map(point => lngLat(asLngLatLiteral(point))) }
        };
    }
    createBacking(map) {
        const backing = { id: this.id, hitId: `${this.id}-hit`, map, owner: this, formerOwner: null, cleanupTimer: null, handlers: [], visibility: 'none' };
        map.polylineBackings.set(backing.id, backing);
        map.map.addSource(backing.id, { type: 'geojson', data: this.feature() });
        map.map.addLayer({ id: backing.id, type: 'line', source: backing.id, layout: { visibility: 'none' }, paint: { 'line-color': ['get', 'color'], 'line-opacity': ['get', 'opacity'], 'line-width': ['get', 'width'] } });
        map.map.addLayer({ id: backing.hitId, type: 'line', source: backing.id, layout: { visibility: 'none' }, paint: { 'line-color': '#000000', 'line-opacity': 0, 'line-width': ['max', ['get', 'width'], 8] } });
        for (const [mapLibreEvent, compatEvent] of POLYLINE_EVENTS) {
            const handler = event => {
                if (mapLibreEvent !== 'mouseleave' && event.point) {
                    // ponytail: only ask MapLibre about the compat hit layers, not the whole style.
                    const hitLayers = map.getCompatHitLayerIds();
                    const topmost = hitLayers.length > 0 && map.map.queryRenderedFeatures(event.point, { layers: hitLayers })
                        .map(feature => map.polylineBackings.get(feature.layer.id.slice(0, -4)))
                        .find(candidate => candidate?.owner?.listeners.get(compatEvent)?.size);
                    if (topmost && topmost !== backing) return;
                }
                if (mapLibreEvent === 'mousemove') event.__compatOverlayHandled = true;
                backing.owner?.emit(compatEvent, { latLng: new CompatLatLng(event.lngLat.lat, event.lngLat.lng) });
            };
            backing.handlers.push([mapLibreEvent, handler]);
            map.map.on(mapLibreEvent, backing.hitId, handler);
        }
        return backing;
    }
    setBackingVisibility(backing, visibility) {
        if (backing.visibility === visibility || !backing.map.map.getLayer(backing.id)) return;
        backing.map.map.setLayoutProperty(backing.id, 'visibility', visibility);
        backing.map.map.setLayoutProperty(backing.hitId, 'visibility', visibility);
        backing.visibility = visibility;
    }
    claimBacking(map) {
        const pool = map.detachedPolylineBackings;
        let backing = this.backing && this.backing.map === map && pool.includes(this.backing) ? this.backing : pool[0];
        if (backing) {
            pool.splice(pool.indexOf(backing), 1);
            clearTimeout(backing.cleanupTimer);
            if (backing.formerOwner && backing.formerOwner !== this) backing.formerOwner.backing = null;
            backing.formerOwner = null;
            backing.owner = this;
            this.id = backing.id;
        } else backing = this.createBacking(map);
        this.backing = backing;
        this.awaitingFirstPublish = true;
        this.setBackingVisibility(backing, 'none');
    }
    removeBacking(backing) {
        if (backing.owner) return;
        const pool = backing.map.detachedPolylineBackings;
        const index = pool.indexOf(backing);
        if (index >= 0) pool.splice(index, 1);
        for (const [event, handler] of backing.handlers) backing.map.map.off(event, backing.hitId, handler);
        if (backing.map.map.getLayer(backing.hitId)) backing.map.map.removeLayer(backing.hitId);
        if (backing.map.map.getLayer(backing.id)) backing.map.map.removeLayer(backing.id);
        if (backing.map.map.getSource(backing.id)) backing.map.map.removeSource(backing.id);
        backing.map.polylineBackings.delete(backing.id);
        if (backing.formerOwner?.backing === backing) backing.formerOwner.backing = null;
    }
    detachBacking() {
        clearTimeout(this.publishTimer);
        this.publishTimer = null;
        this.publishPending = false;
        const backing = this.backing;
        if (!backing) return;
        this.setBackingVisibility(backing, 'none');
        backing.owner = null;
        backing.formerOwner = this;
        if (!backing.map.detachedPolylineBackings.includes(backing)) backing.map.detachedPolylineBackings.push(backing);
        clearTimeout(backing.cleanupTimer);
        backing.cleanupTimer = setTimeout(() => this.removeBacking(backing), POLYLINE_BACKING_TTL_MS);
    }
    setMap(map) {
        if (map === null) {
            this.detachBacking();
            this.map = null;
            return;
        }
        this.map = map;
        map.ready(() => {
            if (this.map !== map) return;
            if (!this.backing?.owner) this.claimBacking(map);
            this.schedulePublish();
        });
    }
    getPath() { return this.path; }
    setPath(path) {
        this.path.__compatArraySubscribers.delete(this.pathChanged);
        this.path = createCompatArray(path);
        this.path.__compatArraySubscribers.add(this.pathChanged);
        this.schedulePublish();
    }
    schedulePublish() {
        if (!this.backing?.owner) return;
        if (this.publishTimer) {
            this.publishPending = true;
            return;
        }
        this.publish();
        this.publishTimer = setTimeout(() => {
            this.publishTimer = null;
            if (this.publishPending) {
                this.publishPending = false;
                this.schedulePublish();
            }
        }, POLYLINE_PATH_DEBOUNCE_MS);
    }
    publish() {
        const backing = this.backing;
        if (!backing || backing.owner !== this) return;
        const source = backing.map.map.getSource(backing.id);
        if (!source) return;
        source.setData(this.feature());
        if (backing.map.map.getLayer(backing.id)) {
            this.awaitingFirstPublish = false;
            this.setBackingVisibility(backing, this.visible ? 'visible' : 'none');
        }
    }
    clear() { this.getPath().clear(); }
    insertAt(index, value) { this.getPath().insertAt(index, value); }
    removeAt(index) { return this.getPath().removeAt(index); }
    setAt(index, value) { this.getPath().setAt(index, value); }
    getMap() { return this.map || null; }
    getValue() { return this.options.item ?? this.options.value; }
    setEditable(value) { this.options.editable = value; }
    setEditingEnabled(value) { this.setEditable(value); }
    getEditable() { return !!this.options.editable; }
    addMouseOutMoveHandler(handler) { return this.addListener('mouseout', handler); }
    setOptions(options = {}) {
        Object.assign(this.options, options);
        if ('visible' in options) this.setVisible(options.visible);
        if ('path' in options) this.setPath(options.path);
        else this.schedulePublish();
    }
    setVisible(visible) {
        this.visible = visible;
        const backing = this.backing;
        if (backing) this.setBackingVisibility(backing, visible && !this.awaitingFirstPublish ? 'visible' : 'none');
    }
    getVisible() { return this.visible; }
}

class CompatOverlayView {
    setMap(map) {
        if (this.map && map === null) {
            if (this._redraw) this.map.map.off('move', this._redraw);
            this.onRemove?.();
            this.map = null;
            return;
        }
        this.map = map;
        map.ready(() => queueMicrotask(() => {
            if (this.map !== map) return;
            this.onAdd?.();
            this.draw?.();
            this._redraw = () => { if (this.map === map) this.draw?.(); };
            map.map.on('move', this._redraw);
        }));
    }
    getPanes() {
        return {
            mapPane: this.map.overlayLayer,
            overlayLayer: this.map.overlayLayer,
            overlayShadow: this.map.overlayLayer,
            overlayImage: this.map.overlayLayer,
            markerLayer: this.map.markerLayer,
            floatPane: this.map.floatPane,
            overlayMouseTarget: this.map.overlayMouseTarget
        };
    }
    getProjection() {
        const project = latLng => {
            const point = this.map.map.project(lngLat(asLngLatLiteral(latLng)));
            return { x: point.x, y: point.y };
        };
        const unproject = point => {
            const lngLatPoint = this.map.map.unproject([point.x, point.y]);
            return new CompatLatLng(lngLatPoint.lat, lngLatPoint.lng);
        };
        return {
            getWorldWidth: () => 512 * Math.pow(2, this.map.map.getZoom()),
            fromLatLngToDivPixel: project,
            fromDivPixelToLatLng: unproject,
            fromContainerPixelToLatLng: unproject,
            fromLatLngToContainerPixel: project
        };
    }
}

class CompatPolygon {
    constructor(options = {}) {
        this.options = options;
        this.visible = options.visible !== false;
        this.listeners = new Map();
        this.id = `compat-polygon-${nextOverlayId++}`;
        this.outerPathsChanged = () => {
            this.subscribeToRings();
            this.publish();
        };
        this.ringChanged = () => this.publish();
        this.ringSubscriptions = new Set();
        this.paths = createCompatArray([]);
        this.setPaths(options.paths ?? (options.path ? [options.path] : []));
        if (options.map) this.setMap(options.map);
    }
    addListener(name, handler) {
        if (!this.listeners.has(name)) this.listeners.set(name, new Set());
        this.listeners.get(name).add(handler);
        return { remove: () => this.listeners.get(name)?.delete(handler) };
    }
    emit(name, event) {
        for (const handler of this.listeners.get(name) || []) handler(event);
    }
    isLatLng(value) {
        return value && ((typeof value.lat === 'function' && typeof value.lng === 'function') ||
            (typeof value.lat === 'number' && typeof value.lng === 'number'));
    }
    normalizePaths(paths) {
        const candidate = createCompatArray(paths || []);
        if (!candidate.getLength()) return candidate;
        const outer = this.isLatLng(candidate.getAt(0)) ? createCompatArray([candidate]) : candidate;
        const values = outer.__compatArrayValues;
        for (let index = 0; index < values.length; index++) values[index] = createCompatArray(values[index]);
        return outer;
    }
    subscribeToRings() {
        for (const ring of this.ringSubscriptions) ring.__compatArraySubscribers.delete(this.ringChanged);
        this.ringSubscriptions.clear();
        const values = this.paths.__compatArrayValues;
        for (let index = 0; index < values.length; index++) {
            const ring = values[index] = createCompatArray(values[index]);
            ring.__compatArraySubscribers.add(this.ringChanged);
            this.ringSubscriptions.add(ring);
        }
    }
    feature() {
        const rings = this.paths.__compatArrayValues.map(path => {
            const ring = createCompatArray(path).__compatArrayValues
                .map(point => lngLat(asLngLatLiteral(point)));
            if (ring.length && (ring[0][0] !== ring.at(-1)[0] || ring[0][1] !== ring.at(-1)[1])) ring.push([...ring[0]]);
            return ring;
        });
        return {
            type: 'Feature',
            properties: {
                strokeColor: this.options.strokeColor || '#000000',
                strokeOpacity: this.options.strokeOpacity ?? 1,
                strokeWeight: this.options.strokeWeight || 1,
                fillColor: this.options.fillColor || this.options.strokeColor || '#000000',
                fillOpacity: this.options.fillOpacity ?? 0
            },
            geometry: { type: 'Polygon', coordinates: rings }
        };
    }
    publish() {
        this.map?.map.getSource(this.id)?.setData(this.feature());
    }
    setMap(map) {
        if (map === null) {
            for (const layer of [`${this.id}-fill`, `${this.id}-line`]) if (this.map?.map.getLayer(layer)) this.map.map.removeLayer(layer);
            if (this.map?.map.getSource(this.id)) this.map.map.removeSource(this.id);
            this.map = null;
            return;
        }
        this.map = map;
        map.ready(() => {
            if (this.map !== map) return;
            map.map.addSource(this.id, { type: 'geojson', data: this.feature() });
            map.map.addLayer({ id: `${this.id}-fill`, type: 'fill', source: this.id, paint: { 'fill-color': ['get', 'fillColor'], 'fill-opacity': ['get', 'fillOpacity'] } });
            map.map.addLayer({ id: `${this.id}-line`, type: 'line', source: this.id, paint: { 'line-color': ['get', 'strokeColor'], 'line-opacity': ['get', 'strokeOpacity'], 'line-width': ['get', 'strokeWeight'] } });
            map.map.on('mouseenter', `${this.id}-fill`, event => this.emit('mouseover', { latLng: new CompatLatLng(event.lngLat.lat, event.lngLat.lng) }));
            map.map.on('mouseleave', `${this.id}-fill`, event => this.emit('mouseout', { latLng: new CompatLatLng(event.lngLat.lat, event.lngLat.lng) }));
            this.setVisible(this.visible);
        });
    }
    getPath() {
        if (!this.paths.getLength()) this.paths.push(createCompatArray([]));
        return this.paths.getAt(0);
    }
    setPath(path) { this.setPaths([path]); }
    getPaths() { return this.paths; }
    setPaths(paths) {
        this.paths.__compatArraySubscribers.delete(this.outerPathsChanged);
        for (const ring of this.ringSubscriptions) ring.__compatArraySubscribers.delete(this.ringChanged);
        this.ringSubscriptions.clear();
        this.paths = this.normalizePaths(paths);
        this.paths.__compatArraySubscribers.add(this.outerPathsChanged);
        this.subscribeToRings();
        this.publish();
    }
    addMouseOutMoveHandler(handler) { return this.addListener('mouseout', handler); }
    setOptions(options = {}) {
        Object.assign(this.options, options);
        if ('visible' in options) this.setVisible(options.visible);
        if ('paths' in options) this.setPaths(options.paths);
        else if ('path' in options) this.setPath(options.path);
        else this.publish();
    }
    setVisible(visible) {
        this.visible = visible;
        for (const layer of [`${this.id}-fill`, `${this.id}-line`]) {
            if (this.map?.map.getLayer(layer)) this.map.map.setLayoutProperty(layer, 'visibility', visible ? 'visible' : 'none');
        }
    }
    getVisible() { return this.visible; }
}

class CompatCircle {
    constructor(options = {}) {
        this.options = options;
        this.center = options.center;
        this.radius = options.radius || 0;
        this.visible = options.visible !== false;
        this.id = `compat-circle-${nextOverlayId++}`;
        if (options.map) this.setMap(options.map);
    }
    feature() {
        return {
            type: 'Feature',
            properties: {
                strokeColor: this.options.strokeColor || '#000000',
                strokeOpacity: this.options.strokeOpacity ?? 1,
                strokeWeight: this.options.strokeWeight || 1,
                fillColor: this.options.fillColor || this.options.strokeColor || '#000000',
                fillOpacity: this.options.fillOpacity ?? 0
            },
            geometry: { type: 'Polygon', coordinates: [circleCoordinates(this.center, this.radius)] }
        };
    }
    setMap(map) {
        if (map === null) {
            for (const layer of [`${this.id}-fill`, `${this.id}-line`]) if (this.map?.map.getLayer(layer)) this.map.map.removeLayer(layer);
            if (this.map?.map.getSource(this.id)) this.map.map.removeSource(this.id);
            this.map = null;
            return;
        }
        this.map = map;
        map.ready(() => {
            map.map.addSource(this.id, { type: 'geojson', data: this.feature() });
            map.map.addLayer({ id: `${this.id}-fill`, type: 'fill', source: this.id, paint: { 'fill-color': ['get', 'fillColor'], 'fill-opacity': ['get', 'fillOpacity'] } });
            map.map.addLayer({ id: `${this.id}-line`, type: 'line', source: this.id, paint: { 'line-color': ['get', 'strokeColor'], 'line-opacity': ['get', 'strokeOpacity'], 'line-width': ['get', 'strokeWeight'] } });
            this.setVisible(this.visible);
        });
    }
    setCenter(center) {
        this.center = center;
        this.map?.map.getSource(this.id)?.setData(this.feature());
    }
    setRadius(radius) {
        this.radius = radius;
        this.map?.map.getSource(this.id)?.setData(this.feature());
    }
    setOptions(options = {}) {
        Object.assign(this.options, options);
        if ('center' in options) this.center = options.center;
        if ('radius' in options) this.radius = options.radius;
        if ('visible' in options) this.setVisible(options.visible);
        this.map?.map.getSource(this.id)?.setData(this.feature());
    }
    setVisible(visible) {
        this.visible = visible;
        for (const layer of [`${this.id}-fill`, `${this.id}-line`]) {
            if (this.map?.map.getLayer(layer)) this.map.map.setLayoutProperty(layer, 'visibility', visible ? 'visible' : 'none');
        }
    }
    getVisible() { return this.visible; }
}

class CompatInfoWindow {
    constructor(options = {}) {
        this.content = options.content || '';
        this.position = options.position;
        this.options = options;
        this.listeners = new Map();
    }
    addListener(name, handler) {
        if (!this.listeners.has(name)) this.listeners.set(name, new Set());
        this.listeners.get(name).add(handler);
        return { remove: () => this.listeners.get(name)?.delete(handler) };
    }
    emit(name) { for (const handler of this.listeners.get(name) || []) handler(); }
    addDomReadyHandler(handler) { return this.addListener('domready', handler); }
    addCloseClickHandler(handler) { return this.addListener('closeclick', handler); }
    open(map, anchor) {
        this.close(false);
        const position = this.position || anchor?.getPosition?.() || map.getCenter();
        const element = document.createElement('div');
        element.className = 'maplibregl-popup maplibregl-popup-anchor-bottom';
        element.style.position = 'absolute';
        const content = document.createElement('div');
        content.className = 'maplibregl-popup-content';
        if (this.content instanceof Node) content.appendChild(this.content);
        else content.innerHTML = this.content;
        const closeButton = document.createElement('button');
        closeButton.className = 'maplibregl-popup-close-button';
        closeButton.type = 'button';
        closeButton.textContent = '×';
        closeButton.addEventListener('click', () => this.close());
        content.appendChild(closeButton);
        element.appendChild(content);
        const draw = () => {
            const point = map.map.project(lngLat(asLngLatLiteral(position)));
            element.style.left = `${point.x}px`;
            element.style.top = `${point.y}px`;
        };
        this.popup = { element, draw, remove: () => element.remove() };
        map.floatPane.appendChild(element);
        map.map.on('move', draw);
        this.popup.off = () => map.map.off('move', draw);
        draw();
        this.emit('domready');
    }
    close(emit = true) { this.popup?.off?.(); this.popup?.remove(); this.popup = null; if (emit) this.emit('closeclick'); }
    setContent(content) { this.content = content; }
    setOptions(options = {}) { Object.assign(this.options, options); if ('content' in options) this.content = options.content; if ('position' in options) this.position = options.position; }
    setPosition(position) { this.position = position; }
}

function createSvgSymbol(pathData, icon = {}) {
    const numbers = [...pathData.matchAll(/-?\d+(?:\.\d+)?/g)].map(match => Number(match[0]));
    const xs = numbers.filter((_, index) => index % 2 === 0);
    const ys = numbers.filter((_, index) => index % 2 === 1);
    const minX = Math.min(...xs, -8);
    const maxX = Math.max(...xs, 8);
    const minY = Math.min(...ys, -8);
    const maxY = Math.max(...ys, 8);
    const padding = Math.max(icon.strokeWeight ?? 1, 1) + 1;
    const width = maxX - minX + padding * 2;
    const height = maxY - minY + padding * 2;
    const scale = icon.scale ?? 1;
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.setAttribute('viewBox', `${minX - padding} ${minY - padding} ${width} ${height}`);
    svg.setAttribute('width', width * scale);
    svg.setAttribute('height', height * scale);
    svg.style.display = 'block';
    const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    path.setAttribute('d', pathData);
    path.setAttribute('fill', icon.fillColor || '#ff0000');
    path.setAttribute('fill-opacity', icon.fillOpacity ?? 1);
    path.setAttribute('stroke', icon.strokeColor || '#111');
    path.setAttribute('stroke-width', icon.strokeWeight ?? 1);
    path.setAttribute('stroke-linejoin', 'round');
    svg.appendChild(path);
    return svg;
}

function createMarkerVisual(icon = {}) {
    if (icon.url) {
        const img = document.createElement('img');
        img.src = icon.url;
        img.style.width = `${icon.scaledSize?.width || icon.size?.width || 18}px`;
        img.style.height = `${icon.scaledSize?.height || icon.size?.height || 18}px`;
        img.style.display = 'block';
        return img;
    }
    if (icon.path === 'forward-closed-arrow') {
        return createArrowSvg(icon.fillColor || '#ff0000', icon.scale || 6, icon.strokeColor || '#fff');
    }
    if (icon.path && icon.path !== 'circle') {
        return createSvgSymbol(icon.path, icon);
    }
    const element = document.createElement('div');
    element.style.width = `${(icon.scale || 6) * 2}px`;
    element.style.height = element.style.width;
    element.style.borderRadius = '50%';
    element.style.background = icon.fillColor || '#ff0000';
    element.style.opacity = icon.fillOpacity ?? 1;
    element.style.border = `${icon.strokeWeight ?? 1}px solid ${icon.strokeColor || '#111'}`;
    return element;
}

class CompatMarker {
    constructor(options = {}) {
        this.options = options;
        this.position = options.position;
        this.title = options.title || '';
        this.visible = options.visible !== false;
        this.element = document.createElement('div');
        this.element.title = this.title;
        this.element.style.lineHeight = '0';
        this.element.style.cursor = 'pointer';
        if (options.zIndex !== undefined) this.element.style.zIndex = options.zIndex;
        this.visual = createMarkerVisual(options.icon || {});
        this.visual.style.transform = `rotate(${options.icon?.rotation || 0}deg)`;
        this.visual.style.transformOrigin = 'center';
        this.element.appendChild(this.visual);
        if (options.map) this.setMap(options.map);
    }
    setMap(map) {
        if (map == null) {
            this.marker?.off?.();
            this.marker?.remove();
            this.marker = null;
            this.map = null;
            return;
        }
        while (map.map?.map) map = map.map;
        this.map = map;
        map.ready(() => {
            let rawMap = map.map;
            while (rawMap?.map) rawMap = rawMap.map;
            Object.assign(this.element.style, { position: 'absolute', zIndex: this.options.zIndex ?? '', pointerEvents: 'auto', transform: 'translate(-50%, -50%)' });
            const draw = () => {
                const point = rawMap.project(lngLat(asLngLatLiteral(this.position)));
                this.element.style.left = `${point.x}px`;
                this.element.style.top = `${point.y}px`;
            };
            this.marker = { remove: () => this.element.remove(), draw };
            map.markerLayer.appendChild(this.element);
            rawMap.on('move', draw);
            this.marker.off = () => rawMap.off('move', draw);
            draw();
            this.setVisible(this.visible);
        });
    }
    setPosition(position) {
        this.position = position;
        this.marker?.draw?.();
    }
    setIcon(icon = {}) {
        this.options.icon = icon;
        const visual = createMarkerVisual(icon);
        visual.style.transform = `rotate(${icon.rotation || 0}deg)`;
        visual.style.transformOrigin = 'center';
        this.visual.replaceWith(visual);
        this.visual = visual;
    }
    getPosition() { return new CompatLatLng(asLngLatLiteral(this.position).lat, asLngLatLiteral(this.position).lng); }
    getIcon() { return this.options.icon; }
    getIcon_MarkerImage() { return this.options.icon; }
    setTitle(title) { this.title = title || ''; this.element.title = this.title; }
    setZindex(zIndex) { this.options.zIndex = zIndex; this.element.style.zIndex = zIndex; }
    setZIndex(zIndex) { this.setZindex(zIndex); }
    setVisible(visible) {
        this.visible = visible;
        this.element.style.display = visible ? '' : 'none';
    }
    getVisible() { return this.visible; }
    setOptions(options = {}) {
        Object.assign(this.options, options);
        if ('visible' in options) this.setVisible(options.visible);
    }
    setAnimation(animation) {
        this.element.classList.toggle('compat-marker-bounce', animation === 'bounce');
    }
    addListener(name, handler) {
        this.element.addEventListener(name, handler);
        return { remove: () => this.element.removeEventListener(name, handler) };
    }
    addClickHandler(handler) { return this.addListener('click', handler); }
    addMarkerMouseOverHandler(handler) { return this.addListener('mouseover', handler); }
    addMarkerMouseOutHandler(handler) { return this.addListener('mouseout', handler); }
}

function ensureCompatStyles() {
    if (document.getElementById('google-maps-maplibre-compat-styles')) return;
    const style = document.createElement('style');
    style.id = 'google-maps-maplibre-compat-styles';
    style.textContent = `@keyframes compat-marker-bounce { 0%, 100% { translate: 0 0; } 50% { translate: 0 -14px; } } .compat-marker-bounce { animation: compat-marker-bounce 450ms ease-in-out infinite; }`;
    document.head.appendChild(style);
}

export function installGoogleMapsCompat() {
    ensureCompatStyles();
    const google = globalThis.google || (globalThis.google = {});
    google.maps = {
        Map: CompatMap,
        LatLng: CompatLatLng,
        LatLngBounds: CompatLatLngBounds,
        Point: CompatPoint,
        Polyline: CompatPolyline,
        Polygon: CompatPolygon,
        Marker: CompatMarker,
        OverlayView: CompatOverlayView,
        Circle: CompatCircle,
        InfoWindow: CompatInfoWindow,
        SymbolPath: { CIRCLE: 'circle', FORWARD_CLOSED_ARROW: 'forward-closed-arrow' },
        Animation: { BOUNCE: 'bounce' },
        RenderingType: { RASTER: 'raster', VECTOR: 'vector', UNINITIALIZED: 'uninitialized' },
        MapTypeId: { ROADMAP: 'roadmap', SATELLITE: 'satellite', HYBRID: 'hybrid' },
        ControlPosition: { RIGHT_BOTTOM: 'RIGHT_BOTTOM', TOP_LEFT: 'TOP_LEFT', TOP_RIGHT: 'TOP_RIGHT', BOTTOM_LEFT: 'BOTTOM_LEFT' },
        event: {
            addListener: (target, name, handler) => target.addListener(name, handler),
            removeListener: (listener) => { if (listener && listener.remove) listener.remove(); },
            clearInstanceListeners: (target) => { if (target && target.listeners instanceof Map) target.listeners.clear(); },
            trigger: (target, name, ...args) => { if (target && typeof target.emit === 'function') target.emit(name, ...args); }
        }
    };
    return google.maps;
}
