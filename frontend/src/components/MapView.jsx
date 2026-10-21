import { useEffect } from 'react';
import { CircleMarker, MapContainer, Polyline, TileLayer, Tooltip, useMap, useMapEvents } from 'react-leaflet';

function Clicks({ onClick }) { useMapEvents({ click: (e) => onClick && onClick(e.latlng) }); return null; }
function Locate() {
  const map = useMap();
  useEffect(() => { navigator.geolocation?.getCurrentPosition((p) => map.setView([p.coords.latitude, p.coords.longitude], 14)); }, [map]);
  return null;
}

/** markers: [{pos:[lat,lng], color, label}] · line: [[lat,lng],[lat,lng]] */
export default function MapView({ markers = [], line, onClick }) {
  return (
    <MapContainer center={[28.61, 77.21]} zoom={12} style={{ height: 430, borderRadius: 12 }}>
      <TileLayer url="https://tile.openstreetmap.org/{z}/{x}/{y}.png" attribution="© OpenStreetMap" />
      <Locate />
      <Clicks onClick={onClick} />
      {markers.map((m) => (
        <CircleMarker key={m.label} center={m.pos} radius={9} pathOptions={{ color: m.color, fillOpacity: 0.8 }}>
          <Tooltip>{m.label}</Tooltip>
        </CircleMarker>
      ))}
      {line && <Polyline positions={line} pathOptions={{ color: '#2563eb' }} />}
    </MapContainer>
  );
}
