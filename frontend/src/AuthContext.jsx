import { createContext, useContext, useEffect, useState } from 'react';
import { api, getToken } from './api.js';

const Ctx = createContext(null);
export const useAuth = () => useContext(Ctx);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    (async () => {
      const q = new URLSearchParams(window.location.search).get('token');      // Google OAuth redirect: /?token=...
      if (q) { localStorage.setItem('token', q); window.history.replaceState({}, '', '/' + window.location.hash); }
      if (getToken()) { try { setUser(await api('/me')); } catch { localStorage.removeItem('token'); } }
      setReady(true);
    })();
  }, []);

  const authenticate = async (path, body) => {
    const r = await api(path, 'POST', body);
    localStorage.setItem('token', r.token);
    setUser(await api('/me'));
  };
  const logout = () => { localStorage.removeItem('token'); setUser(null); };

  return <Ctx.Provider value={{ user, ready, authenticate, logout }}>{children}</Ctx.Provider>;
}
