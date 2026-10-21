import { useCallback, useEffect, useState } from 'react';
import { api, money, useWs } from '../api.js';
import MapView from '../components/MapView.jsx';
import { Btn, Card } from '../components/ui.jsx';
import { useSafe, useToast } from '../components/Toast.jsx';

const NEXT = { ACCEPTED: ['arrived', "I've arrived"], ARRIVED: ['start', 'Start ride'], IN_PROGRESS: ['complete', 'Complete ride'] };

export default function Driver() {
  const toast = useToast(), safe = useSafe();
  const [d, setD] = useState(null), [reqs, setReqs] = useState([]), [hist, setHist] = useState([]), [ratings, setRatings] = useState([]);
  const [me, setMe] = useState(null);

  const load = useCallback(async () => {
    try {
      const [dash, rq, h, rt] = await Promise.all([api('/driver/dashboard'), api('/driver/requests'), api('/driver/history'), api('/driver/ratings')]);
      setD(dash); setReqs(rq); setHist(h); setRatings(rt);
    } catch (e) { toast(e.message); }
  }, [toast]);
  useEffect(() => { load(); const t = setInterval(load, 8000); return () => clearInterval(t); }, [load]);

  useWs(d ? [[`/topic/driver/${d.driverId}`, (v) => { toast('New ride request #' + v.id); load(); }]] : [], [d?.driverId]);

  const setLoc = safe(async (ll) => { await api('/driver/location', 'PUT', { lat: ll.lat, lng: ll.lng }); setMe([ll.lat, ll.lng]); load(); });
  const post = (path) => safe(async () => { await api(path, 'POST'); load(); });
  if (!d) return <p>Loading…</p>;
  const a = d.activeRide;
  const markers = [...(me ? [{ pos: me, color: 'blue', label: 'You' }] : []),
    ...(a ? [{ pos: [a.pickupLat, a.pickupLng], color: 'green', label: 'Pickup' }, { pos: [a.dropLat, a.dropLng], color: 'red', label: 'Drop' }] : [])];

  return (
    <div className="grid md:grid-cols-2 gap-3">
      <Card><MapView markers={markers} onClick={setLoc} /><p className="text-sm mt-1">Click the map to set your current location (a real app would stream GPS).</p></Card>
      <div className="space-y-3">
        <Card title="Dashboard">
          <p>Earnings <b>{money(d.earnings)}</b> · Rides {d.completedRides} · ⭐ {d.avgRating} ({d.ratingCount})</p>
          <Btn color={d.available ? 'red' : 'green'} onClick={post(`/driver/online?available=${!d.available}`)}>{d.available ? 'Go offline' : 'Go online'}</Btn>
        </Card>
        {a && (
          <Card title={`Active ride #${a.id}: ${a.status}`}>
            <p className="text-sm">{a.rider} · {a.distanceKm} km · {money(a.fare)}</p>
            {NEXT[a.status] && <Btn color="green" onClick={post(`/driver/rides/${a.id}/${NEXT[a.status][0]}`)}>{NEXT[a.status][1]}</Btn>}
          </Card>
        )}
        <Card title="Ride requests">
          {reqs.length === 0 && <p className="text-sm">None{d.available ? '' : ' – go online to receive requests'}</p>}
          {reqs.map((r) => (
            <div key={r.id} className="border-b py-1 text-sm">#{r.id} · {r.distanceKm} km · {money(r.fare)} · {r.rider}<br />
              <Btn color="green" onClick={post(`/driver/rides/${r.id}/accept`)}>Accept</Btn><Btn color="red" onClick={post(`/driver/rides/${r.id}/reject`)}>Reject</Btn></div>
          ))}
        </Card>
        <Card title="Ratings">
          <div className="text-sm">{ratings.length === 0 ? 'No ratings yet' : ratings.map((r) => <div key={r.rideId}>⭐{r.stars} {r.comment}</div>)}</div>
        </Card>
        <Card title="Ride history"><div className="text-sm">{hist.map((r) => <div key={r.id}>#{r.id} {r.status} {money(r.fare)}</div>)}</div></Card>
      </div>
    </div>
  );
}
