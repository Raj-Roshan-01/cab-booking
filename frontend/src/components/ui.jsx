const colors = { dark: 'bg-slate-900', green: 'bg-green-600', red: 'bg-red-600', blue: 'bg-blue-600' };
export const Card = ({ title, children, className = '' }) => (
  <div className={`bg-white rounded-xl shadow p-4 ${className}`}>{title && <h3 className="font-bold mb-2">{title}</h3>}{children}</div>
);
export const Btn = ({ color = 'dark', className = '', ...p }) => (
  <button {...p} className={`${colors[color]} text-white rounded px-3 py-1 mr-2 mt-1 disabled:opacity-50 ${className}`} />
);
