import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export const getToken = () => localStorage.getItem('token');
export const money = (n) => '₹' + Number(n ?? 0).toFixed(2);

export async function api(path, method = 'GET', body) {
  const res = await fetch('/api' + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(getToken() ? { Authorization: 'Bearer ' + getToken() } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!res.ok) {
    const e = await res.json().catch(() => ({}));
    throw new Error(e.error || 'Request failed (' + res.status + ')');
  }
  return (res.headers.get('content-type') || '').includes('json') ? res.json() : res.blob();
}

export async function downloadReceipt(id) {
  const blob = await api(`/rides/${id}/receipt`);
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob); a.download = `receipt-${id}.pdf`; a.click();
}

/** Subscribe to STOMP topics. `subs` = [[destination, handler], ...]; reconnects when `deps` change. */
export function useWs(subs, deps) {
  const ref = useRef(subs);
  ref.current = subs;
  useEffect(() => {
    if (!subs.length) return;
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: 'Bearer ' + getToken() },
      reconnectDelay: 5000,
      onConnect: () => ref.current.forEach(([dest, _], i) => client.subscribe(dest, (m) => ref.current[i][1](JSON.parse(m.body)))),
    });
    client.activate();
    return () => { client.deactivate(); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);
}
