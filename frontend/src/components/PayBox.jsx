import { useMemo, useState } from 'react';
import { loadStripe } from '@stripe/stripe-js';
import { CardElement, Elements, useElements, useStripe } from '@stripe/react-stripe-js';
import { api, money } from '../api.js';
import { Btn } from './ui.jsx';
import { useToast } from './Toast.jsx';

function StripeForm({ ride, onPaid }) {
  const stripe = useStripe(), els = useElements(), toast = useToast();
  const [busy, setBusy] = useState(false);
  const pay = async () => {
    setBusy(true);
    try {
      const i = await api(`/payments/ride/${ride.id}/intent`, 'POST');
      const r = await stripe.confirmCardPayment(i.clientSecret, { payment_method: { card: els.getElement(CardElement) } });
      if (r.error) throw new Error(r.error.message);
      toast('Payment submitted – waiting for confirmation');
      setTimeout(onPaid, 2500);                      // the Stripe webhook marks the payment as paid
    } catch (e) { toast(e.message); }
    setBusy(false);
  };
  return (
    <div><div className="border rounded p-2 my-2"><CardElement /></div>
      <Btn color="green" disabled={busy || !stripe} onClick={pay}>Pay {money(ride.fare)}</Btn></div>
  );
}

export default function PayBox({ ride, cfg, onPaid }) {
  const toast = useToast();
  const stripePromise = useMemo(() => (cfg.demo ? null : loadStripe(cfg.publishableKey)), [cfg]);
  if (cfg.demo) {
    const demo = async () => {
      try {
        await api(`/payments/ride/${ride.id}/intent`, 'POST');
        await api(`/payments/ride/${ride.id}/demo-confirm`, 'POST');
        toast('Payment successful – receipt generated'); onPaid();
      } catch (e) { toast(e.message); }
    };
    return <Btn color="green" onClick={demo}>Pay {money(ride.fare)} (demo)</Btn>;
  }
  return <Elements stripe={stripePromise}><StripeForm ride={ride} onPaid={onPaid} /></Elements>;
}
