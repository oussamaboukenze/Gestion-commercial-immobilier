import { Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography, LinearProgress } from '@mui/material';
import type { ReactNode } from 'react';

export interface Colonne<T> {
  titre: string;
  rendu: (ligne: T) => ReactNode;
  align?: 'left' | 'right' | 'center';
  largeur?: number | string;
}

interface Props<T> {
  colonnes: Colonne<T>[];
  lignes: T[] | undefined;
  cle: (ligne: T) => string | number;
  chargement?: boolean;
  vide?: string;
  onClick?: (ligne: T) => void;
}

/** Tableau simple réutilisé par tous les écrans. */
export default function DataTable<T>({ colonnes, lignes, cle, chargement, vide = 'Aucune donnée', onClick }: Props<T>) {
  return (
    <TableContainer component={Paper} variant="outlined">
      {chargement && <LinearProgress />}
      <Table size="small">
        <TableHead>
          <TableRow>
            {colonnes.map((c) => (
              <TableCell key={c.titre} align={c.align} sx={{ fontWeight: 600, width: c.largeur, whiteSpace: 'nowrap' }}>
                {c.titre}
              </TableCell>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {(lignes ?? []).map((l) => (
            <TableRow key={cle(l)} hover={!!onClick} onClick={onClick ? () => onClick(l) : undefined}
              sx={{ cursor: onClick ? 'pointer' : 'default' }}>
              {colonnes.map((c) => <TableCell key={c.titre} align={c.align}>{c.rendu(l)}</TableCell>)}
            </TableRow>
          ))}
          {!chargement && (lignes ?? []).length === 0 && (
            <TableRow>
              <TableCell colSpan={colonnes.length}>
                <Typography variant="body2" color="text.secondary" align="center" sx={{ py: 2 }}>{vide}</Typography>
              </TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
