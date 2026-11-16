import { Alert, Box, Button, Card, CardContent, Grid, LinearProgress, Link, Stack, Typography } from '@mui/material';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import { useQuery } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { useParams } from 'react-router-dom';
import { ApiError, api, ouvrirDocument, qs } from '../../api/client';
import type {
  Compromis, DossierNotaire, Encaissement, Lot, Page, PieceJointe, RelanceClient, Reservation,
} from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import DataTable from '../../components/DataTable';
import { useDialogue } from '../../components/Dialogue';
import FormDialog from '../../components/FormDialog';
import { useNotifier } from '../../components/Notifier';
import PageHeader from '../../components/PageHeader';
import StatutChip from '../../components/StatutChip';
import Televersement from '../../components/Televersement';
import { useAction } from '../../components/useAction';
import { aujourdhui, date, dateHeure, dh, libelle } from '../../format';
import { CHAMPS_ENCAISSEMENT } from '../tresorerie/Encaissements';

function Info({ label, valeur }: { label: string; valeur: ReactNode }) {
  return (
    <Box sx={{ mb: 1 }}>
      <Typography variant="caption" color="text.secondary">{label}</Typography>
      <Typography variant="body2" component="div">{valeur}</Typography>
    </Box>
  );
}

function Piece({ ok, label }: { ok: boolean; label: string }) {
  return (
    <Stack direction="row" spacing={0.5} alignItems="center">
      {ok ? <CheckIcon color="success" fontSize="small" /> : <CloseIcon color="error" fontSize="small" />}
      <Typography variant="body2">{label}</Typography>
    </Stack>
  );
}

/** Dossier de vente complet : réservation → compromis → encaissements → notaire. */
export default function ReservationDetail() {
  const { id } = useParams();
  const { hasRole } = useAuth();
  const notifier = useNotifier();
  const adv = hasRole('ADV');
  const comptable = hasRole('COMPTABLE');
  const voitTresorerie = hasRole('ADV', 'COMPTABLE', 'DAF', 'DIRECTION');
  const voitNotaire = hasRole('ADV', 'COMPTABLE', 'DAF', 'DIRECTION');

  const resa = useQuery({ queryKey: ['reservation', id], queryFn: () => api.get<Reservation>(`/api/ventes/reservations/${id}`) });
  const r = resa.data;
  const compromis = useQuery({
    queryKey: ['echeancier', id], enabled: !!r && (r.statut === 'VALIDEE' || r.statut === 'COMPROMIS_SIGNE' || r.statut === 'DESISTEE'),
    queryFn: () => api.get<Compromis>(`/api/ventes/reservations/${id}/echeancier`).catch((e) => {
      if (e instanceof ApiError && e.status === 404) return null;
      throw e;
    }),
  });
  const pieces = useQuery({ queryKey: ['pieces', id], queryFn: () => api.get<PieceJointe[]>(`/api/ventes/reservations/${id}/pieces`) });
  const encaissements = useQuery({ queryKey: ['encaissements-dossier', id], enabled: voitTresorerie,
    queryFn: () => api.get<Encaissement[]>(`/api/tresorerie/dossiers/${id}/encaissements`) });
  const relances = useQuery({ queryKey: ['relances-dossier', id], enabled: hasRole('ADV', 'COMPTABLE', 'DAF'),
    queryFn: () => api.get<RelanceClient[]>(`/api/tresorerie/dossiers/${id}/relances`) });
  const notaire = useQuery({
    queryKey: ['notaire-dossier', id], enabled: voitNotaire && r?.statut === 'COMPROMIS_SIGNE',
    queryFn: () => api.get<DossierNotaire>(`/api/postvente/notaire/reservations/${id}`).catch((e) => {
      if (e instanceof ApiError && e.status === 404) return null;
      throw e;
    }),
  });
  const lotsDispo = useQuery({
    queryKey: ['lots-dispo', r?.projetCode], enabled: adv && !!r,
    queryFn: () => api.get<Page<Lot>>('/api/referentiel/lots' + qs({ projet: r!.projetCode, statut: 'DISPONIBLE', size: 200 })),
  });

  const valider = useAction(() => api.post(`/api/ventes/reservations/${id}/valider`), 'Réservation validée : lot réservé');
  const rejeter = useAction((motif: unknown) => api.post(`/api/ventes/reservations/${id}/rejeter`, { motif }), 'Réservation rejetée');
  const remise = useAction((ok: boolean) => api.post(`/api/ventes/reservations/${id}/remise/${ok ? 'accorder' : 'refuser'}`), 'Décision enregistrée');
  const majPieces = useAction((v: Record<string, unknown>) => api.put(`/api/ventes/reservations/${id}/pieces`, v), 'Pièces mises à jour');
  const etablir = useAction(() => api.post('/api/ventes/compromis', { reservationId: Number(id) }), 'Compromis établi, échéancier généré');
  const signer = useAction((cid: number) => api.post(`/api/ventes/compromis/${cid}/signer`), 'Compromis signé');
  const dateEch = useAction((v: { id: number; d: unknown }) => api.put(`/api/ventes/echeances/${v.id}/date`, { dateEcheance: v.d }), 'Date enregistrée');
  const desister = useAction((v: Record<string, unknown>) => api.post('/api/ventes/desistements', { ...v, reservationId: Number(id) }),
    'Désistement saisi : en attente de validation');
  const changer = useAction((v: Record<string, unknown>) => api.post('/api/ventes/changements-lot', { ...v, reservationId: Number(id) }),
    'Changement de lot envoyé au responsable commercial');
  const encaisser = useAction((v: Record<string, unknown>) => api.post('/api/tresorerie/encaissements', { ...v, reservationId: Number(id) }),
    'Encaissement enregistré, reçu généré');
  const ouvrirNotaire = useAction((v: Record<string, unknown>) => api.post('/api/postvente/notaire/dossiers', { ...v, reservationId: Number(id) }),
    'Dossier notaire ouvert');

  const dRejet = useDialogue();
  const dPieces = useDialogue();
  const dDate = useDialogue<number>();
  const dDesist = useDialogue();
  const dChangement = useDialogue();
  const dEnc = useDialogue();
  const dNotaire = useDialogue();

  const doc = (chemin: string) => ouvrirDocument(chemin).catch((e: Error) => notifier(e.message, 'error'));

  if (resa.isLoading) return <LinearProgress />;
  if (!r) return <Alert severity="error">{resa.error?.message ?? 'Réservation introuvable'}</Alert>;

  const actif = ['EN_ATTENTE_VALIDATION_ADV', 'VALIDEE', 'COMPROMIS_SIGNE'].includes(r.statut);
  const remiseEnAttente = r.statutRemise.startsWith('EN_ATTENTE');
  const peutDeciderRemise = (r.statutRemise === 'EN_ATTENTE_RESP_COMMERCIAL' && hasRole('RESP_COMMERCIAL', 'DIRECTION'))
    || (r.statutRemise === 'EN_ATTENTE_DIRECTION' && hasRole('DIRECTION'));
  const c = compromis.data;
  const avancement = Math.min(100, (r.totalEncaisse * 100) / r.prixNet);

  return (
    <Box>
      <PageHeader titre={`Dossier ${r.numero}`} sousTitre={`${r.clientNom} · lot ${r.lotCode} · commercial ${r.commercial}`}
        actions={<StatutChip statut={r.statut} />} />

      {r.statut === 'EN_ATTENTE_VALIDATION_ADV' && (
        <Alert severity={r.avanceConfirmee && r.pieceCin && r.pieceJustificatifRevenus && r.pieceRecuAvance && !remiseEnAttente ? 'success' : 'warning'} sx={{ mb: 2 }}
          action={adv && (
            <Stack direction="row" spacing={1}>
              <Button color="inherit" size="small" variant="outlined" onClick={() => valider.mutate()}>Valider</Button>
              <Button color="inherit" size="small" onClick={() => dRejet.ouvrir()}>Rejeter</Button>
            </Stack>
          )}>
          Contrôle ADV : pièces {r.pieceCin && r.pieceJustificatifRevenus && r.pieceRecuAvance ? 'complètes' : 'incomplètes'} ·
          avance {r.avanceConfirmee ? 'confirmée par la trésorerie' : `non confirmée (${dh(r.totalEncaisse)} / ${dh(r.avance)})`}
          {remiseEnAttente && ` · remise de ${r.remisePct} % en attente (${libelle(r.statutRemise)})`}
        </Alert>
      )}
      {r.motifRejet && <Alert severity="error" sx={{ mb: 2 }}>Rejetée : {r.motifRejet}</Alert>}

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 4 }}>
          <Card variant="outlined"><CardContent>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>Client & lot</Typography>
            <Info label="Client" valeur={`${r.clientNom} — CIN ${r.clientCin}`} />
            <Info label="Téléphone" valeur={r.clientTelephone || '—'} />
            <Info label="Lot" valeur={`${r.lotCode} (projet ${r.projetCode})`} />
            <Info label="Créée le" valeur={dateHeure(r.dateCreation)} />
            {r.dateValidation && <Info label="Validée le" valeur={`${dateHeure(r.dateValidation)} par ${r.validateurAdv}`} />}
            {r.dateLimiteCompromis && <Info label="Compromis avant le" valeur={date(r.dateLimiteCompromis)} />}
          </CardContent></Card>
        </Grid>
        <Grid size={{ xs: 12, md: 4 }}>
          <Card variant="outlined"><CardContent>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>Prix & paiements</Typography>
            <Info label="Prix TTC" valeur={dh(r.prixTTC)} />
            <Info label="Remise" valeur={<>{r.remisePct} % <StatutChip statut={r.statutRemise} /></>} />
            <Info label="Prix net" valeur={<b>{dh(r.prixNet)}</b>} />
            <Info label="Avance prévue (min. 5 %)" valeur={`${dh(r.avance)} (min. ${dh(r.avanceMinimale)})`} />
            <Info label="Total encaissé" valeur={`${dh(r.totalEncaisse)} — reste ${dh(r.prixNet - r.totalEncaisse)}`} />
            <LinearProgress variant="determinate" value={avancement} sx={{ height: 8, borderRadius: 4 }} />
            {peutDeciderRemise && (
              <Stack direction="row" spacing={1} sx={{ mt: 2 }}>
                <Button size="small" variant="contained" onClick={() => remise.mutate(true)}>Accorder la remise</Button>
                <Button size="small" color="error" onClick={() => remise.mutate(false)}>Refuser</Button>
              </Stack>
            )}
          </CardContent></Card>
        </Grid>
        <Grid size={{ xs: 12, md: 4 }}>
          <Card variant="outlined"><CardContent>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>Pièces du dossier</Typography>
            <Piece ok={r.pieceCin} label="CIN" />
            <Piece ok={r.pieceJustificatifRevenus} label="Justificatif de revenus" />
            <Piece ok={r.pieceRecuAvance} label="Reçu de l'avance" />
            {(pieces.data ?? []).map((p) => (
              <Typography key={p.id} variant="caption" display="block">
                📎 <Link component="button" onClick={() => doc(`/api/ventes/pieces/${p.id}/fichier`)}>{p.nomFichier}</Link> ({libelle(p.type)})
              </Typography>
            ))}
            {r.statut === 'EN_ATTENTE_VALIDATION_ADV' && hasRole('COMMERCIAL', 'RESP_COMMERCIAL', 'ADV') && (
              <Stack spacing={1} sx={{ mt: 2 }}>
                <Televersement reservationId={r.id} />
                <Button size="small" onClick={() => dPieces.ouvrir(undefined, { pieceCin: r.pieceCin, pieceJustificatifRevenus: r.pieceJustificatifRevenus, pieceRecuAvance: r.pieceRecuAvance })}>
                  Cocher manuellement
                </Button>
              </Stack>
            )}
          </CardContent></Card>
        </Grid>
      </Grid>

      {/* ---------- Actions ADV ---------- */}
      <Stack direction="row" spacing={1} sx={{ my: 2 }} flexWrap="wrap" useFlexGap>
        {r.statut !== 'EN_ATTENTE_VALIDATION_ADV' && r.statut !== 'REJETEE' &&
          <Button variant="outlined" onClick={() => doc(`/api/ventes/reservations/${r.id}/bon-reservation`)}>Bon de réservation</Button>}
        {adv && r.statut === 'VALIDEE' && compromis.data === null &&
          <Button variant="contained" onClick={() => etablir.mutate()}>Etablir le compromis</Button>}
        {c && <Button variant="outlined" onClick={() => doc(`/api/ventes/compromis/${c.id}/document`)}>Compromis & échéancier</Button>}
        {adv && c?.statut === 'BROUILLON' &&
          <Button variant="contained" onClick={() => signer.mutate(c.id)}>Compromis signé</Button>}
        {comptable && actif && <Button variant="contained" color="secondary" onClick={() => dEnc.ouvrir(undefined, { mode: 'CHEQUE', dateEncaissement: aujourdhui() })}>Encaisser</Button>}
        {adv && actif && <Button onClick={() => dChangement.ouvrir()}>Changement de lot</Button>}
        {adv && actif && <Button color="error" onClick={() => dDesist.ouvrir()}>Désistement</Button>}
        {adv && r.statut === 'COMPROMIS_SIGNE' && notaire.data === null &&
          <Button variant="outlined" onClick={() => dNotaire.ouvrir()}>Ouvrir le dossier notaire</Button>}
      </Stack>

      {c && (
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>
            Echéancier — compromis <StatutChip statut={c.statut} />
            {c.statut === 'BROUILLON' && <Typography component="span" variant="body2" color="text.secondary">
              {' '}· versement requis à la signature : {dh(c.versementRequisSignature)} (encaissé : {dh(c.totalEncaisse)})</Typography>}
          </Typography>
          <DataTable cle={(e) => e.id} lignes={c.echeances}
            colonnes={[
              { titre: '#', rendu: (e) => e.ordre },
              { titre: 'Echéance', rendu: (e) => e.libelle },
              { titre: '%', rendu: (e) => `${e.pourcentage} %`, align: 'right' },
              { titre: 'Montant', rendu: (e) => dh(e.montant), align: 'right' },
              { titre: 'Payé', rendu: (e) => dh(e.montantPaye), align: 'right' },
              { titre: 'Reste', rendu: (e) => dh(e.resteAPayer), align: 'right' },
              { titre: 'Exigible le', rendu: (e) => date(e.dateEcheance) },
              { titre: 'Etat', rendu: (e) => <StatutChip statut={e.etat} /> },
              { titre: '', rendu: (e) => adv && e.etat !== 'PAYEE' && <Button size="small" onClick={() => dDate.ouvrir(e.id, { d: e.dateEcheance ?? '' })}>Date</Button> },
            ]} />
        </Box>
      )}

      {notaire.data && (
        <Alert severity="info" sx={{ mb: 2 }}>
          Dossier notaire chez {notaire.data.notaire} : <StatutChip statut={notaire.data.statut} />
          {notaire.data.numeroActe && ` · acte n° ${notaire.data.numeroActe} du ${date(notaire.data.dateSignatureActe)}`}
        </Alert>
      )}

      {voitTresorerie && (
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>Encaissements</Typography>
          <DataTable cle={(e) => e.id} lignes={encaissements.data} chargement={encaissements.isLoading} vide="Aucun encaissement"
            colonnes={[
              { titre: 'Reçu', rendu: (e) => <Link component="button" onClick={() => doc(`/api/tresorerie/encaissements/${e.id}/recu`)}>{e.numeroRecu}</Link> },
              { titre: 'Date', rendu: (e) => date(e.dateEncaissement) },
              { titre: 'Mode', rendu: (e) => `${libelle(e.mode)}${e.numeroCheque ? ` n° ${e.numeroCheque}` : ''}` },
              { titre: 'Montant', rendu: (e) => dh(e.montant), align: 'right' },
              { titre: 'Statut', rendu: (e) => <StatutChip statut={e.statut} /> },
            ]} />
        </Box>
      )}
      {(relances.data?.length ?? 0) > 0 && (
        <Box>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>Relances</Typography>
          <DataTable cle={(x) => x.id} lignes={relances.data}
            colonnes={[
              { titre: 'Niveau', rendu: (x) => libelle(x.niveau) },
              { titre: 'Objet', rendu: (x) => x.objet },
              { titre: 'Montant dû', rendu: (x) => dh(x.montantDu), align: 'right' },
              { titre: 'Statut', rendu: (x) => <StatutChip statut={x.statut} /> },
              { titre: 'Créée le', rendu: (x) => date(x.dateCreation) },
            ]} />
        </Box>
      )}

      {/* ---------- Boîtes de dialogue ---------- */}
      <FormDialog ouvert={dRejet.ouvert} titre="Rejeter la réservation" onFermer={dRejet.fermer} danger libelleValider="Rejeter"
        champs={[{ nom: 'motif', label: 'Motif', type: 'textarea', requis: true }]}
        onValider={(v) => rejeter.mutate(v.motif, { onSuccess: dRejet.fermer })} />
      <FormDialog ouvert={dPieces.ouvert} titre="Pièces fournies" initial={dPieces.initial} onFermer={dPieces.fermer}
        champs={[{ nom: 'pieceCin', label: 'CIN', type: 'checkbox' }, { nom: 'pieceJustificatifRevenus', label: 'Justificatif de revenus', type: 'checkbox' },
          { nom: 'pieceRecuAvance', label: "Reçu de l'avance", type: 'checkbox' }]}
        onValider={(v) => majPieces.mutate(v, { onSuccess: dPieces.fermer })} />
      <FormDialog ouvert={dDate.ouvert} titre="Date d'exigibilité de l'échéance" initial={dDate.initial} onFermer={dDate.fermer}
        texte="Ex. date du constat d'achèvement du gros oeuvre. Les relances partent automatiquement à J+7, J+15 et J+30."
        champs={[{ nom: 'd', label: 'Date', type: 'date', requis: true }]}
        onValider={(v) => dateEch.mutate({ id: dDate.cible!, d: v.d }, { onSuccess: dDate.fermer })} />
      <FormDialog ouvert={dDesist.ouvert} titre="Désistement (annulation de la vente)" onFermer={dDesist.fermer} danger libelleValider="Saisir le désistement"
        texte={r.statut === 'COMPROMIS_SIGNE'
          ? 'Après compromis : montant à rembourser selon les clauses du compromis. Validation responsable commercial + DAF.'
          : 'Avant compromis : remboursement des versements moins 10 % de frais de dossier. Validation du responsable commercial.'}
        champs={[{ nom: 'motif', label: 'Motif', type: 'textarea', requis: true },
          ...(r.statut === 'COMPROMIS_SIGNE' ? [{ nom: 'montantSelonClauses', label: 'Montant à rembourser (DH)', type: 'number' as const, requis: true }] : [])]}
        onValider={(v) => desister.mutate(v, { onSuccess: dDesist.fermer })} />
      <FormDialog ouvert={dChangement.ouvert} titre="Changement de lot" onFermer={dChangement.fermer} libelleValider="Envoyer pour validation"
        texte="L'ancien lot sera libéré et les versements transférés après validation du responsable commercial."
        champs={[{ nom: 'nouveauLotId', label: 'Nouveau lot (disponible)', type: 'select', requis: true,
          options: (lotsDispo.data?.content ?? []).map((l) => ({ valeur: l.id, label: `${l.code} · ${l.typologie} · ${dh(l.prixTTC)}` })) },
          { nom: 'motif', label: 'Motif', type: 'textarea' }]}
        onValider={(v) => changer.mutate(v, { onSuccess: dChangement.fermer })} />
      <FormDialog ouvert={dEnc.ouvert} titre={`Encaissement — ${r.numero}`} initial={dEnc.initial} onFermer={dEnc.fermer}
        champs={CHAMPS_ENCAISSEMENT.filter((ch) => ch.nom !== 'reservationId')} libelleValider="Encaisser"
        onValider={(v) => encaisser.mutate(v, { onSuccess: dEnc.fermer })} />
      <FormDialog ouvert={dNotaire.ouvert} titre="Ouvrir le dossier notaire" onFermer={dNotaire.fermer}
        champs={[{ nom: 'notaire', label: 'Notaire partenaire', requis: true }, { nom: 'observations', label: 'Observations', type: 'textarea' }]}
        onValider={(v) => ouvrirNotaire.mutate(v, { onSuccess: dNotaire.fermer })} />
    </Box>
  );
}
