export const MAPLIBRE_CSS = 'https://unpkg.com/maplibre-gl@5.9.0/dist/maplibre-gl.css';
export const MAPLIBRE_JS = 'https://unpkg.com/maplibre-gl@5.9.0/dist/maplibre-gl.js';

export const MARSEILLE_CENTER = { lat: 43.275, lng: 5.322 };
export const RACE_VECTOR_STYLE_URL = 'https://tiles.openfreemap.org/styles/liberty';
export const RACE_WATER_COLOR = '#00437d';

export function lngLat(point) {
    const lng = typeof point.lng === 'function' ? point.lng() : point.lng;
    const lat = typeof point.lat === 'function' ? point.lat() : point.lat;
    return [lng, lat];
}

function toMapLibreZoom(googleZoom) {
    return googleZoom - 1;
}

export function createRaceStyle() {
    return RACE_VECTOR_STYLE_URL;
}

export function applyRaceStyle(map) {
    const apply = () => {
        for (const layer of map.getStyle().layers || []) {
            if (layer.type === 'fill' && /water/i.test(`${layer.id} ${layer['source-layer'] || ''}`)) {
                map.setPaintProperty(layer.id, 'fill-color', RACE_WATER_COLOR);
            }
            if (layer.type === 'line' && /waterway/i.test(`${layer.id} ${layer['source-layer'] || ''}`)) {
                map.setPaintProperty(layer.id, 'line-color', '#2b7eb3');
            }
        }
        if (!map.getSource('openseamap')) {
            map.addSource('openseamap', {
                type: 'raster',
                tiles: ['https://tiles.openseamap.org/seamark/{z}/{x}/{y}.png'],
                tileSize: 256,
                attribution: '© OpenSeaMap contributors',
                maxzoom: 18
            });
        }
        if (!map.getLayer('openseamap')) {
            map.addLayer({ id: 'openseamap', type: 'raster', source: 'openseamap', paint: { 'raster-opacity': 0.75 } });
        }
    };
    if (map.loaded()) apply();
    else map.once('load', apply);
}

export function createRaceMap(containerId, options = {}) {
    const center = options.center || MARSEILLE_CENTER;
    const map = new maplibregl.Map({
        container: containerId,
        style: createRaceStyle(),
        center: lngLat(center),
        zoom: toMapLibreZoom(options.zoom ?? 15),
        bearing: options.bearing ?? 0,
        pitch: 0
    });
    map.addControl(new maplibregl.NavigationControl({ visualizePitch: false }), 'top-right');
    applyRaceStyle(map);
    return map;
}

export function createArrowSvg(color = '#ff0000', scale = 6, strokeColor = '#fff') {
    const size = scale * 6;
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.setAttribute('viewBox', '-18 -18 36 36');
    svg.setAttribute('width', size);
    svg.setAttribute('height', size);
    svg.style.display = 'block';
    svg.style.filter = 'drop-shadow(0 0 1px #111)';
    const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    path.setAttribute('d', 'M 0 -16 L 11 12 L 0 6 L -11 12 Z');
    path.setAttribute('fill', color);
    path.setAttribute('stroke', strokeColor);
    path.setAttribute('stroke-width', '2');
    path.setAttribute('stroke-linejoin', 'round');
    svg.appendChild(path);
    return svg;
}

export function lineFeature(id, points, properties = {}) {
    return {
        type: 'Feature',
        id,
        properties: { id, ...properties },
        geometry: { type: 'LineString', coordinates: points.map(lngLat) }
    };
}

export function polygonFeature(id, rings, properties = {}) {
    return {
        type: 'Feature',
        id,
        properties: { id, ...properties },
        geometry: { type: 'Polygon', coordinates: rings.map(ring => ring.map(lngLat)) }
    };
}

export function lineCollection(features) {
    return { type: 'FeatureCollection', features };
}

export function setTestState(patch) {
    window.__testState = { ...(window.__testState || {}), ...patch };
    return window.__testState;
}

export function generateBoatTrack(boatIdx, totalSteps = 100) {
    const track = [];
    let lat = 43.270;
    let lng = 5.320 + boatIdx * 0.001;
    let heading = boatIdx % 2 === 0 ? 45 : -45;
    const tackInterval = 8 + boatIdx;
    for (let i = 0; i < totalSteps; i++) {
        if (i > 0 && i % tackInterval === 0) {
            heading = heading > 0 ? -45 : 45;
        }
        const stepSize = 0.00035 + boatIdx * 0.00001;
        const rad = heading * Math.PI / 180;
        lat += stepSize * Math.cos(rad);
        lng += stepSize * Math.sin(rad) * 0.65;
        track.push({ lat, lng, heading: heading + 90, speed: 5 + boatIdx * 0.5 });
    }
    return track;
}

export function makeCircle(center, radiusDeg, steps = 64) {
    const ring = [];
    for (let i = 0; i <= steps; i++) {
        const angle = (i / steps) * Math.PI * 2;
        ring.push({
            lat: center.lat + Math.cos(angle) * radiusDeg,
            lng: center.lng + Math.sin(angle) * radiusDeg
        });
    }
    return ring;
}
