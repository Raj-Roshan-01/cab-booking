import { useCallback, useEffect, useState } from 'react';
import { api, downloadReceipt, money, useWs } from '../api.js';
import MapView from '../components/MapView.jsx';
import PayBox from '../components/PayBox.jsx';
import { Btn, Card } from '../components/ui.jsx';
import { useSafe, useToast } from '../components/Toast.jsx';

const ACTIVE = ['REQUESTED', 'ACCEPTED', 'ARRIVED', 'IN_PROGRESS'];

export default function Rider() {
  const toast = useToast(), safe = useSafe();
  const [sel, setSel] = useState({});               // { p, d, est }
  const [promo, setPromo] = useState('');
  const [rides, setRides] = useState([]);
  const [car, setCar] = useState(null);
  const [cfg, setCfg] = useState(null);
  const [rating, setRating] = useState({ stars: 5, comment: '' });
  const [ticket, setTicket] = useState({ subject: '', message: '' });

  const load = useCallback(() => api('/rides').then(setRides).catch((e) => toast(e.message)), [toast]);
  useEffect(() => { load(); api('/payments/config').then(setCfg); }, [load]);

  const cur = rides.find((r) => ACTIVE.includes(r.status)) || rides.find((r) => r.status === 'COMPLETED' && (!r.paid || !r.rated));

  useWs(cur ? [
    [`/topic/ride/${cur.id}`, (v) => { toast('Ride ' + v.status.replace('_', ' ').toLowerCase()); load(); }],
    [`/topic/ride/${cur.id}/location`, setCar],
    [`/topic/ride/${cur.id}/sos`, () => toast('SOS alert sent')],
  ] : [], [cur?.id]);

  const pick = safe(async (ll) => {
    if (cur) return;
    if (!sel.p || sel.d) return setSel({ p: [ll.lat, ll.lng] });
    const d = [ll.lat, ll.lng];
    const est = await api('/rides/estimate', 'POST', { pickupLat: sel.p[0], pickupLng: sel.p[1], dropLat: d[0], dropLng: d[1] });
    setSel({ ...sel, d, est });
  });

  const book = safe(async () => {
    if (!sel.d) throw new Error('Pick pickup and drop on the map first');
    await api('/rides', 'POST', {
      pickupLat: sel.p[0], pickupLng: sel.p[1], dropLat: sel.d[0], dropLng: sel.d[1],
      pickupAddress: `Pin ${sel.p[0].toFixed(4)}, ${sel.p[1].toFixed(4)}`, dropAddress: `Pin ${sel.d[0].toFixed(4)}, ${sel.d[1].toFixed(4)}`, promoCode: promo,
    });
    setSel({}); load();
  });
  const act = (path) => safe(async () => { await api(path, 'POST'); load(); });
  const sendRating = safe(async () => { await api(`/rides/${cur.id}/rating`, 'POST', rating); toast('Thanks for rating!'); load(); });
  const sendTicket = safe(async () => { await api('/support', 'POST', ticket); toast('Ticket submitted'); setTicket({ subject: '', message: '' }); });

  const target = cur || sel;
  const markers = cur
    ? [{ pos: [cur.pickupLat, cur.pickupLng], color: 'green', label: 'Pickup' }, { pos: [cur.dropLat, cur.dropLng], color: 'red', label: 'Drop' },
       ...(car ? [{ pos: [car.lat, car.lng], color: 'blue', label: 'Driver' }] : [])]
    : [...(sel.p ? [{ pos: sel.p, color: 'green', label: 'Pickup' }] : []), ...(sel.d ? [{ pos: sel.d, color: 'red', label: 'Drop' }] : [])];

  return (
    <div className="grid md:grid-cols-2 gap-3">
      <Card><MapView markers={markers} line={sel.d && !cur ? [sel.p, sel.d] : undefined} onClick={pick} />
        <p className="text-sm mt-1">Click the map: 1st click = pickup, 2nd click = drop.</p></Card>
      <div className="space-y-3">
        {!cur && (
          <Card title="Book a ride">
            <p className="text-sm my-1">{sel.est ? `${sel.est.distanceKm} km · ETA ${sel.est.etaMinutes} min · ${money(sel.est.fare)}` : 'Select pickup & drop on the map.'}</p>
            <input placeholder="Promo code (try WELCOME10)" value={promo} onChange={(e) => setPromo(e.target.value)} />
            <Btn onClick={book}>Book now</Btn>
          </Card>
        )}
        {cur && (
          <Card title={`Ride #${cur.id}: ${cur.status}`}>
            <p className="text-sm">{cur.distanceKm} km · ETA {cur.etaMinutes} min · {money(cur.fare)}{cur.discount ? ` (saved ${money(cur.discount)})` : ''}</p>
            <p className="my-1">{cur.driverName ? `Driver: ${cur.driverName} · ${cur.vehicle} · ${cur.plate}` : 'Finding nearby drivers…'}</p>
            {['REQUESTED', 'ACCEPTED', 'ARRIVED'].includes(cur.status) && <Btn color="red" onClick={act(`/rides/${cur.id}/cancel`)}>Cancel</Btn>}
            {cur.driverId && cur.status !== 'COMPLETED' && <Btn color="red" onClick={safe(async () => { await api(`/rides/${cur.id}/sos`, 'POST'); toast('SOS broadcast to admins'); })}>🆘 SOS</Btn>}
            {cur.status === 'COMPLETED' && !cur.paid && cfg && <PayBox ride={cur} cfg={cfg} onPaid={load} />}
            {cur.status === 'COMPLETED' && cur.paid && !cur.rated && (
              <div className="mt-2 space-y-1">
                <select value={rating.stars} onChange={(e) => setRating({ ...rating, stars: +e.target.value })}>{[5, 4, 3, 2, 1].map((n) => <option key={n} value={n}>{'⭐'.repeat(n)}</option>)}</select>
                <input placeholder="Feedback" value={rating.comment} onChange={(e) => setRating({ ...rating, comment: e.target.value })} />
                <Btn onClick={sendRating}>Rate driver</Btn>
              </div>
            )}
          </Card>
        )}
        <Card title="Ride history">
          <div className="text-sm">
            {rides.length === 0 && 'No rides yet'}
            {rides.map((r) => (
              <div key={r.id} className="border-b py-1">#{r.id} {r.status} {money(r.fare)}{' '}
                <button className="underline" onClick={safe(() => downloadReceipt(r.id))}>PDF</button>{' '}
                <button className="underline" onClick={safe(async () => { await api(`/rides/${r.id}/email-receipt`, 'POST'); toast('Receipt e-mail queued'); })}>Email</button></div>
            ))}
          </div>
        </Card>
        <Card title="Support">
          <input placeholder="Subject" value={ticket.subject} onChange={(e) => setTicket({ ...ticket, subject: e.target.value })} />
          <textarea className="mt-1" placeholder="Message" value={ticket.message} onChange={(e) => setTicket({ ...ticket, message: e.target.value })} />
          <Btn onClick={sendTicket}>Send</Btn>
        </Card>
      </div>
    </div>
  );
}
