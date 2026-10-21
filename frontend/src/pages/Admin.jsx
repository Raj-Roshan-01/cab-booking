import { useCallback, useEffect, useState } from 'react';
import { api, money, useWs } from '../api.js';
import { Btn, Card } from '../components/ui.jsx';
import { useSafe, useToast } from '../components/Toast.jsx';

export default function Admin() {
  const toast = useToast(), safe = useSafe();
  const [stats, setStats] = useState({}), [promos, setPromos] = useState([]), [tickets, setTickets] = useState([]), [rank, setRank] = useState([]), [users, setUsers] = useState([]);
  const [sos, setSos] = useState([]), [promo, setPromo] = useState({ code: '', percentOff: 10 });

  const load = useCallback(async () => {
    try {
      const [s, p, t, r, u] = await Promise.all([api('/admin/stats'), api('/admin/promos'), api('/admin/tickets'), api('/drivers/ranking'), api('/admin/users')]);
      setStats(s); setPromos(p); setTickets(t); setRank(r); setUsers(u);
    } catch (e) { toast(e.message); }
  }, [toast]);
  useEffect(() => { load(); const t = setInterval(load, 10000); return () => clearInterval(t); }, [load]);

  useWs([['/topic/sos', (a) => { toast(`🆘 SOS on ride #${a.rideId} by ${a.by}`); setSos((x) => [a, ...x]); }]], []);

  const addPromo = safe(async () => { await api('/admin/promos', 'POST', { code: promo.code, percentOff: +promo.percentOff }); setPromo({ code: '', percentOff: 10 }); load(); });
  const resolve = (id) => safe(async () => { await api(`/admin/tickets/${id}/resolve`, 'POST'); load(); });

  return (
    <div className="grid md:grid-cols-2 gap-3">
      <Card title="System overview" className="md:col-span-2">
        {Object.entries(stats).map(([k, v]) => <span key={k} className="inline-block mr-4">{k}: <b>{k === 'revenue' ? money(v) : v}</b></span>)}
      </Card>
      <Card title="🆘 Live SOS alerts"><div className="text-sm text-red-600">{sos.length === 0 ? <span className="text-slate-500">None</span> : sos.map((a, i) => <div key={i}>Ride #{a.rideId} — {a.by} — {a.time}</div>)}</div></Card>
      <Card title="Driver ranking"><div className="text-sm">{rank.length === 0 ? 'No ratings yet' : rank.map((r) => <div key={r.rank}>{r.rank}. {r.driver} ⭐{r.avgRating} ({r.ratings})</div>)}</div></Card>
      <Card title="Promo codes">
        <div className="text-sm mb-2">{promos.map((p) => <div key={p.id}>{p.code} — {p.percentOff}% off</div>)}</div>
        <div className="flex gap-2"><input placeholder="CODE" value={promo.code} onChange={(e) => setPromo({ ...promo, code: e.target.value })} />
          <input type="number" value={promo.percentOff} onChange={(e) => setPromo({ ...promo, percentOff: e.target.value })} /></div>
        <Btn onClick={addPromo}>Add promo</Btn>
      </Card>
      <Card title="Support tickets">
        <div className="text-sm">{tickets.length === 0 ? 'None' : tickets.map((t) => (
          <div key={t.id} className="border-b py-1">#{t.id} [{t.status}] {t.user}: <b>{t.subject}</b> — {t.message}{' '}
            {t.status === 'OPEN' && <button className="underline" onClick={resolve(t.id)}>Resolve</button>}</div>))}</div>
      </Card>
      <Card title="Users" className="md:col-span-2"><div className="text-sm">{users.map((u) => <div key={u.id}>{u.name} · {u.email} · {u.role}</div>)}</div></Card>
    </div>
  );
}
