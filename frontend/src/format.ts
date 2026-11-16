const nf = new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

export const dh = (v: number | null | undefined) => (v === null || v === undefined ? '—' : `${nf.format(v)} DH`);
export const date = (v: string | null | undefined) => (v ? new Date(v).toLocaleDateString('fr-FR') : '—');
export const dateHeure = (v: string | null | undefined) =>
  v ? new Date(v).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' }) : '—';
export const aujourdhui = () => new Date().toISOString().slice(0, 10);
export const libelle = (v: string | null | undefined) =>
  v ? v.replace(/_/g, ' ').toLowerCase().replace(/^\w/, (c) => c.toUpperCase()) : '—';
