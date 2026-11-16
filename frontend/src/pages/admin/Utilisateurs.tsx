import { Alert, Box, Button, Chip, Stack, TextField } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { api, qs } from '../../api/client';
import type { UtilisateurAdmin } from '../../api/types';
import DataTable from '../../components/DataTable';
import { useDialogue } from '../../components/Dialogue';
import FormDialog from '../../components/FormDialog';
import PageHeader from '../../components/PageHeader';
import { useAction } from '../../components/useAction';
import { libelle } from '../../format';

const PROFILS = ['COMMERCIAL', 'ADV', 'COMPTABLE', 'ADMIN_SI', 'RESP_COMMERCIAL', 'DAF', 'SAV', 'DIRECTION'];

/** Administration > Utilisateurs / Profils & droits (moindre privilège, désactivation jamais suppression). */
export default function Utilisateurs() {
  const [q, setQ] = useState('');
  const [motDePasse, setMotDePasse] = useState<{ username: string; mdp: string } | null>(null);
  const users = useQuery({ queryKey: ['admin-users', q], queryFn: () => api.get<UtilisateurAdmin[]>('/api/admin/utilisateurs' + qs({ q })) });
  const projets = useQuery({ queryKey: ['admin-projets'], queryFn: () => api.get<string[]>('/api/admin/projets') });
  const dNouveau = useDialogue();
  const dDroits = useDialogue<UtilisateurAdmin>();
  const creer = useAction((v: Record<string, unknown>) => api.post<{ utilisateur: UtilisateurAdmin; motDePasseTemporaire: string }>('/api/admin/utilisateurs', v), 'Utilisateur créé');
  const droits = useAction((v: { id: string; corps: Record<string, unknown> }) => api.put(`/api/admin/utilisateurs/${v.id}/droits`, v.corps), 'Droits mis à jour');
  const activer = useAction((v: { id: string; actif: boolean }) => api.post(`/api/admin/utilisateurs/${v.id}/${v.actif ? 'activer' : 'desactiver'}`), 'Compte mis à jour');
  const reinit = useAction((u: UtilisateurAdmin) => api.post<{ motDePasseTemporaire: string }>(`/api/admin/utilisateurs/${u.id}/reinitialiser-mot-de-passe`), 'Mot de passe réinitialisé');
  const optionsProjets = (projets.data ?? []).map((p) => ({ valeur: p, label: p }));

  return (
    <Box>
      <PageHeader titre="Utilisateurs" sousTitre="Un départ = désactivation (jamais suppression) + réaffectation des prospects dans le CRM"
        actions={<Button variant="contained" startIcon={<AddIcon />} onClick={() => dNouveau.ouvrir(undefined, { projets: [] })}>Nouveau</Button>} />
      {motDePasse && (
        <Alert severity="info" sx={{ mb: 2 }} onClose={() => setMotDePasse(null)}>
          Mot de passe temporaire de <b>{motDePasse.username}</b> : <code>{motDePasse.mdp}</code> — à communiquer une seule fois, changement imposé à la 1re connexion.
        </Alert>
      )}
      <TextField size="small" label="Rechercher" value={q} onChange={(e) => setQ(e.target.value)} sx={{ mb: 2, width: 300 }} />
      <DataTable cle={(u) => u.id} lignes={users.data} chargement={users.isLoading}
        colonnes={[
          { titre: 'Identifiant', rendu: (u) => <b>{u.username}</b> },
          { titre: 'Nom', rendu: (u) => `${u.prenom ?? ''} ${u.nom ?? ''}` },
          { titre: 'Email', rendu: (u) => u.email },
          { titre: 'Profil', rendu: (u) => (u.profil ? libelle(u.profil) : '—') },
          { titre: 'Projets', rendu: (u) => <Stack direction="row" spacing={0.5}>{u.projets.map((p) => <Chip key={p} label={p} size="small" />)}</Stack> },
          { titre: 'Etat', rendu: (u) => (u.actif ? <Chip label="Actif" color="success" size="small" /> : <Chip label="Désactivé" size="small" />) },
          {
            titre: '', rendu: (u) => (
              <Stack direction="row" spacing={1}>
                <Button size="small" onClick={() => dDroits.ouvrir(u, { profil: u.profil, projets: u.projets })}>Droits</Button>
                <Button size="small" onClick={() => reinit.mutate(u, { onSuccess: (r) => setMotDePasse({ username: u.username, mdp: r.motDePasseTemporaire }) })}>Mot de passe</Button>
                <Button size="small" color={u.actif ? 'error' : 'success'} onClick={() => activer.mutate({ id: u.id, actif: !u.actif })}>
                  {u.actif ? 'Désactiver' : 'Réactiver'}
                </Button>
              </Stack>
            ),
          },
        ]} />
      <FormDialog ouvert={dNouveau.ouvert} titre="Nouvel utilisateur" initial={dNouveau.initial} onFermer={dNouveau.fermer} enCours={creer.isPending}
        champs={[{ nom: 'username', label: 'Identifiant', requis: true, aide: 'minuscules, ex. y.alaoui' }, { nom: 'prenom', label: 'Prénom', requis: true },
          { nom: 'nom', label: 'Nom', requis: true }, { nom: 'email', label: 'Email', type: 'email', requis: true },
          { nom: 'profil', label: 'Profil', type: 'select', options: PROFILS, requis: true },
          { nom: 'projets', label: 'Projets autorisés', type: 'multiselect', options: optionsProjets }]}
        onValider={(v) => creer.mutate(v, { onSuccess: (r) => { dNouveau.fermer(); setMotDePasse({ username: r.utilisateur.username, mdp: r.motDePasseTemporaire }); } })} />
      <FormDialog ouvert={dDroits.ouvert} titre={`Droits de ${dDroits.cible?.username ?? ''}`} initial={dDroits.initial} onFermer={dDroits.fermer}
        champs={[{ nom: 'profil', label: 'Profil', type: 'select', options: PROFILS, requis: true },
          { nom: 'projets', label: 'Projets autorisés', type: 'multiselect', options: optionsProjets }]}
        onValider={(v) => droits.mutate({ id: dDroits.cible!.id, corps: v }, { onSuccess: dDroits.fermer })} />
    </Box>
  );
}
