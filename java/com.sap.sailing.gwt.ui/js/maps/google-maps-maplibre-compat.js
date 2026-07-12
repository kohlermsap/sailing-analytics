import { applyRaceStyle, createArrowSvg, createRaceStyle, lngLat } from './maplibre-test-utils.js';

function asLngLatLiteral(value) {
    if (Array.isArray(value)) return { lat: value[1], lng: value[0] };
    if (typeof value.lat === 'function') return { lat: value.lat(), lng: value.lng() };
    return value;
}

function toMapLibreZoom(googleZoom) {
    return googleZoom - 1;
}

function toGoogleZoom(mapLibreZoom) {
    return mapLibreZoom + 1;
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
        this.cameraChangedSinceIdle = true;
        element.style.position = element.style.position || 'relative';
        this.overlayLayer = document.createElement('div');
        this.overlayMouseTarget = document.createElement('div');
        Object.assign(this.overlayLayer.style, { position: 'absolute', inset: '0', zIndex: 5, pointerEvents: 'none' });
        Object.assign(this.overlayMouseTarget.style, { position: 'absolute', inset: '0', zIndex: 10, pointerEvents: 'none' });
        element.append(this.overlayLayer, this.overlayMouseTarget);
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
        this.resizeObserver = new ResizeObserver(() => this.map.resize());
        this.resizeObserver.observe(element);
        const controlPositions = {
            1: ['10px', '', '', '10px'], 2: ['10px', '', '', '50%'], 3: ['10px', '10px', '', ''],
            4: ['50%', '', '', '10px'], 5: ['60px', '', '', '10px'], 6: ['', '', '60px', '10px'],
            7: ['60px', '10px', '', ''], 8: ['50%', '10px', '', ''], 9: ['', '10px', '60px', ''],
            10: ['', '', '10px', '10px'], 11: ['', '', '10px', '50%'], 12: ['', '10px', '10px', '']
        };
        this.controls = Array.from({ length: 13 }, (_, position) => {
            const container = document.createElement('div');
            const [top, right, bottom, left] = controlPositions[position] || ['', '', '', ''];
            Object.assign(container.style, { position: 'absolute', top, right, bottom, left, zIndex: 20, pointerEvents: 'auto' });
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
        applyRaceStyle(this.map);
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
            this.map.on(name, event => this.emit(name, { latLng: new CompatLatLng(event.lngLat.lat, event.lngLat.lng) }));
        }
        this.map.on('dragstart', event => queueMicrotask(() => this.emit('dragstart', { latLng: new CompatLatLng(event.lngLat.lat, event.lngLat.lng) })));
        this.map.on('contextmenu', event => this.emit('rightclick', { latLng: new CompatLatLng(event.lngLat.lat, event.lngLat.lng) }));
        this.map.on('load', () => {
            this.loaded = true;
            this.deferred.splice(0).forEach(action => action());
        });
    }
    addListener(name, handler) {
        if (!this.listeners.has(name)) this.listeners.set(name, new Set());
        this.listeners.get(name).add(handler);
        return { remove: () => this.listeners.get(name)?.delete(handler) };
    }
    emit(name, event = {}) {
        for (const handler of this.listeners.get(name) || []) handler(event);
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
    setZoom(zoom) { this.map.setZoom(toMapLibreZoom(zoom)); }
    getCenter() {
        const center = this.map.getCenter();
        return new CompatLatLng(center.lat, center.lng);
    }
    setCenter(center) { this.map.setCenter(lngLat(asLngLatLiteral(center))); }
    fitBounds(bounds) { this.map.fitBounds(bounds.toMapLibreBounds()); }
    panTo(position) { this.map.panTo(lngLat(asLngLatLiteral(position))); }
    setHeading(degrees) { this.map.rotateTo(degrees, { duration: 500, easing: t => t * (2 - t) }); }
    getHeading() { return (this.map.getBearing() + 360) % 360; }
    setMapTypeId(mapTypeId) { this.options.mapTypeId = mapTypeId; }
    getMapTypeId() { return this.options.mapTypeId || 'roadmap'; }
    setOptions(options = {}) { Object.assign(this.options, options); }
    resize() { this.map.resize(); this.emit('resize'); }
}

let nextOverlayId = 1;
const POLYLINE_PATH_DEBOUNCE_MS = globalThis.__maplibreCompatPathDebounceMs ?? 25;
const POLYLINE_BACKING_TTL_MS = globalThis.__maplibreCompatBackingTtlMs ?? 10000;
const POLYLINE_EVENTS = [
    ['mouseenter', 'mouseover'], ['mousemove', 'mousemove'], ['mouseleave', 'mouseout'],
    ['mousedown', 'mousedown'], ['mouseup', 'mouseup'], ['click', 'click']
];

class CompatPolyline {
    constructor(options = {}) {
        this.options = options;
        this.path = options.path?.toArray ? options.path.toArray() : (options.path || []);
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
            geometry: { type: 'LineString', coordinates: this.path.map(point => lngLat(asLngLatLiteral(point))) }
        };
    }
    createBacking(map) {
        const backing = { id: this.id, map, owner: this, formerOwner: null, cleanupTimer: null, handlers: [] };
        map.map.addSource(backing.id, { type: 'geojson', data: this.feature() });
        map.map.addLayer({ id: backing.id, type: 'line', source: backing.id, layout: { visibility: 'none' }, paint: { 'line-color': ['get', 'color'], 'line-opacity': ['get', 'opacity'], 'line-width': ['get', 'width'] } });
        for (const [mapLibreEvent, compatEvent] of POLYLINE_EVENTS) {
            const handler = event => backing.owner?.emit(compatEvent, { latLng: new CompatLatLng(event.lngLat.lat, event.lngLat.lng) });
            backing.handlers.push([mapLibreEvent, handler]);
            map.map.on(mapLibreEvent, backing.id, handler);
        }
        return backing;
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
        map.map.setLayoutProperty(backing.id, 'visibility', 'none');
    }
    removeBacking(backing) {
        if (backing.owner) return;
        const pool = backing.map.detachedPolylineBackings;
        const index = pool.indexOf(backing);
        if (index >= 0) pool.splice(index, 1);
        for (const [event, handler] of backing.handlers) backing.map.map.off(event, backing.id, handler);
        if (backing.map.map.getLayer(backing.id)) backing.map.map.removeLayer(backing.id);
        if (backing.map.map.getSource(backing.id)) backing.map.map.removeSource(backing.id);
        if (backing.formerOwner?.backing === backing) backing.formerOwner.backing = null;
    }
    detachBacking() {
        clearTimeout(this.publishTimer);
        const backing = this.backing;
        if (!backing) return;
        backing.map.map.setLayoutProperty(backing.id, 'visibility', 'none');
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
    pathArray() {
        const polyline = this;
        return {
            getLength: () => polyline.path.length,
            getAt: index => polyline.path[index],
            get: index => polyline.path[index],
            push: value => { polyline.path.push(value); polyline.schedulePublish(); return polyline.path.length; },
            insertAt: (index, value) => { polyline.path.splice(index, 0, value); polyline.schedulePublish(); },
            removeAt: index => { const removed = polyline.path.splice(index, 1)[0]; polyline.schedulePublish(); return removed; },
            setAt: (index, value) => { polyline.path[index] = value; polyline.schedulePublish(); },
            clear: () => { polyline.path.splice(0); polyline.schedulePublish(); },
            forEach: callback => polyline.path.forEach((value, index) => callback(value, index)),
            toArray: () => [...polyline.path]
        };
    }
    getPath() { return this.pathArray(); }
    setPath(path) {
        this.path = path?.toArray ? path.toArray() : [...path];
        this.schedulePublish();
    }
    schedulePublish() {
        clearTimeout(this.publishTimer);
        if (!this.backing?.owner) return;
        this.publishTimer = setTimeout(() => this.publish(), POLYLINE_PATH_DEBOUNCE_MS);
    }
    publish() {
        const backing = this.backing;
        if (!backing || backing.owner !== this) return;
        const source = backing.map.map.getSource(backing.id);
        if (!source) return;
        source.setData(this.feature());
        if (backing.map.map.getLayer(backing.id)) {
            backing.map.map.setPaintProperty(backing.id, 'line-color', this.options.strokeColor || '#000000');
            backing.map.map.setPaintProperty(backing.id, 'line-opacity', this.options.strokeOpacity ?? 1);
            backing.map.map.setPaintProperty(backing.id, 'line-width', this.options.strokeWeight || 1);
            this.awaitingFirstPublish = false;
            backing.map.map.setLayoutProperty(backing.id, 'visibility', this.visible ? 'visible' : 'none');
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
        this.schedulePublish();
    }
    setVisible(visible) {
        this.visible = visible;
        const backing = this.backing;
        if (backing?.map.map.getLayer(backing.id)) backing.map.map.setLayoutProperty(backing.id, 'visibility', visible && !this.awaitingFirstPublish ? 'visible' : 'none');
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
            floatPane: this.map.overlayMouseTarget,
            overlayMouseTarget: this.map.overlayMouseTarget
        };
    }
    getProjection() {
        return {
            getWorldWidth: () => 512 * Math.pow(2, this.map.map.getZoom()),
            fromLatLngToDivPixel: latLng => {
                const point = this.map.map.project(lngLat(asLngLatLiteral(latLng)));
                return { x: point.x, y: point.y };
            },
            fromDivPixelToLatLng: point => {
                const lngLatPoint = this.map.map.unproject([point.x, point.y]);
                return new CompatLatLng(lngLatPoint.lat, lngLatPoint.lng);
            },
            fromContainerPixelToLatLng: point => {
                const lngLatPoint = this.map.map.unproject([point.x, point.y]);
                return new CompatLatLng(lngLatPoint.lat, lngLatPoint.lng);
            },
            fromLatLngToContainerPixel: latLng => {
                const point = this.map.map.project(lngLat(asLngLatLiteral(latLng)));
                return { x: point.x, y: point.y };
            }
        };
    }
}

class CompatPolygon {
    constructor(options = {}) {
        this.options = options;
        this.paths = options.paths || [];
        this.visible = true;
        this.listeners = new Map();
        this.id = `compat-polygon-${nextOverlayId++}`;
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
        const ring = this.paths.map(point => lngLat(asLngLatLiteral(point)));
        if (ring.length && (ring[0][0] !== ring.at(-1)[0] || ring[0][1] !== ring.at(-1)[1])) ring.push(ring[0]);
        return {
            type: 'Feature',
            properties: {
                strokeColor: this.options.strokeColor || '#000000',
                strokeOpacity: this.options.strokeOpacity ?? 1,
                strokeWeight: this.options.strokeWeight || 1,
                fillColor: this.options.fillColor || this.options.strokeColor || '#000000',
                fillOpacity: this.options.fillOpacity ?? 0
            },
            geometry: { type: 'Polygon', coordinates: [ring] }
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
            map.map.on('mouseenter', `${this.id}-fill`, event => this.emit('mouseover', { latLng: new CompatLatLng(event.lngLat.lat, event.lngLat.lng) }));
            map.map.on('mouseleave', `${this.id}-fill`, event => this.emit('mouseout', { latLng: new CompatLatLng(event.lngLat.lat, event.lngLat.lng) }));
            this.setVisible(this.visible);
        });
    }
    getPath() { return { getAt: index => this.paths[index], getLength: () => this.paths.length, toArray: () => [...this.paths] }; }
    setPath(path) { this.paths = path?.toArray ? path.toArray() : path; this.map?.map.getSource(this.id)?.setData(this.feature()); }
    addMouseOutMoveHandler(handler) { return this.addListener('mouseout', handler); }
    setOptions(options = {}) {
        Object.assign(this.options, options);
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

class CompatCircle {
    constructor(options = {}) {
        this.options = options;
        this.center = options.center;
        this.radius = options.radius || 0;
        this.visible = true;
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
        element.style.zIndex = 20;
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
        map.element.appendChild(element);
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
        this.visible = true;
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
        if (map === null) {
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
            Object.assign(this.element.style, { position: 'absolute', zIndex: this.options.zIndex ?? '', transform: 'translate(-50%, -50%)' });
            const draw = () => {
                const point = rawMap.project(lngLat(asLngLatLiteral(this.position)));
                this.element.style.left = `${point.x}px`;
                this.element.style.top = `${point.y}px`;
            };
            this.marker = { remove: () => this.element.remove(), draw };
            map.element.appendChild(this.element);
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
    getIcon_MarkerImage() { return this.options.icon; }
    setTitle(title) { this.title = title || ''; this.element.title = this.title; }
    setZindex(zIndex) { this.options.zIndex = zIndex; this.element.style.zIndex = zIndex; }
    setZIndex(zIndex) { this.setZindex(zIndex); }
    setVisible(visible) {
        this.visible = visible;
        this.element.style.display = visible ? '' : 'none';
    }
    getVisible() { return this.visible; }
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
    globalThis.google = { maps: {
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
    } };
    return globalThis.google.maps;
}
