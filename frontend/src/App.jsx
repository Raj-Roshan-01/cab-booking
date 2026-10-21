import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './AuthContext.jsx';
import Login from './pages/Login.jsx';
import Rider from './pages/Rider.jsx';
import Driver from './pages/Driver.jsx';
import Admin from './pages/Admin.jsx';

const HOME = { RIDER: '/rider', DRIVER: '/driver', ADMIN: '/admin' };

function Guard({ role, children }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (user.role !== role) return <Navigate to={HOME[user.role]} replace />;
  return children;
}

export default function App() {
  const { user, ready, logout } = useAuth();
  if (!ready) return <p className="p-6">Loading…</p>;
  return (
    <div className="min-h-screen bg-slate-100 text-slate-800">
      <header className="bg-slate-900 text-white p-3 flex justify-between items-center">
        <b>🚖 CabGo</b>
        {user && <span>{user.name} ({user.role}) <button className="underline ml-3" onClick={logout}>Logout</button></span>}
      </header>
      <main className="max-w-6xl mx-auto p-3">
        <Routes>
          <Route path="/login" element={user ? <Navigate to={HOME[user.role]} replace /> : <Login />} />
          <Route path="/rider" element={<Guard role="RIDER"><Rider /></Guard>} />
          <Route path="/driver" element={<Guard role="DRIVER"><Driver /></Guard>} />
          <Route path="/admin" element={<Guard role="ADMIN"><Admin /></Guard>} />
          <Route path="*" element={<Navigate to={user ? HOME[user.role] : '/login'} replace />} />
        </Routes>
      </main>
    </div>
  );
}
