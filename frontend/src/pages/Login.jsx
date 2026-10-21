import { useState } from 'react';
import { useAuth } from '../AuthContext.jsx';
import { useSafe } from '../components/Toast.jsx';
import { Btn, Card } from '../components/ui.jsx';

export default function Login() {
  const { authenticate } = useAuth();
  const safe = useSafe();
  const [f, setF] = useState({ name: '', email: '', password: '', phone: '', vehicleModel: '', plate: '' });
  const [role, setRole] = useState('rider');
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const login = safe(() => authenticate('/auth/login', { email: f.email, password: f.password }));
  const register = safe(() => authenticate('/auth/register/' + role, f));

  return (
    <Card title="Login / Register" className="max-w-md mx-auto space-y-2">
      <input placeholder="Name (register)" value={f.name} onChange={set('name')} />
      <input placeholder="Email" value={f.email} onChange={set('email')} />
      <input type="password" placeholder="Password (min 8)" value={f.password} onChange={set('password')} />
      <input placeholder="Phone" value={f.phone} onChange={set('phone')} />
      <select value={role} onChange={(e) => setRole(e.target.value)}><option value="rider">Rider</option><option value="driver">Driver</option></select>
      {role === 'driver' && <>
        <input placeholder="Vehicle model" value={f.vehicleModel} onChange={set('vehicleModel')} />
        <input placeholder="Plate number" value={f.plate} onChange={set('plate')} />
      </>}
      <div><Btn onClick={login}>Login</Btn><Btn color="green" onClick={register}>Register</Btn>
        <a className="text-blue-600 underline text-sm" href="/oauth2/authorization/google">Sign in with Google</a></div>
      <p className="text-xs text-slate-500">Admin demo: admin@cab.com / Admin@123</p>
    </Card>
  );
}
