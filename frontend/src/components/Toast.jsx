import { createContext, useCallback, useContext, useState } from 'react';

const Ctx = createContext(() => {});
export const useToast = () => useContext(Ctx);

/** Wraps an async handler so errors become toasts. */
export const useSafe = () => {
  const toast = useToast();
  return (fn) => async (...a) => { try { await fn(...a); } catch (e) { toast(e.message); } };
};

export function ToastProvider({ children }) {
  const [items, setItems] = useState([]);
  const toast = useCallback((msg) => {
    const id = Math.random();
    setItems((x) => [...x, { id, msg }]);
    setTimeout(() => setItems((x) => x.filter((i) => i.id !== id)), 4500);
  }, []);
  return (
    <Ctx.Provider value={toast}>
      {children}
      <div className="fixed top-3 right-3 z-[9999] space-y-2">
        {items.map((i) => <div key={i.id} className="bg-black text-white px-3 py-2 rounded shadow">{i.msg}</div>)}
      </div>
    </Ctx.Provider>
  );
}
