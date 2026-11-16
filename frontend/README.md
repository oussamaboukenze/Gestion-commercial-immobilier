# GECIMMO — Back-office React

Interface web des équipes AHI : ADV, trésorerie, responsables commerciaux, DAF, direction, SAV et admin SI.
Les menus affichés dépendent du profil Keycloak de l'utilisateur.

**Stack** : React 19 · TypeScript · Vite · MUI · TanStack Query · keycloak-js (PKCE)

## Lancer

Pré-requis : backend démarré (voir `../backend/README.md`) et Node.js 20+.

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
```

Connexion avec un utilisateur de test (`adv1`, `comptable1`, `resp1`, `daf1`, `direction1`, `admin1`, `sav1`,
`commercial1`), mot de passe `Gecimmo2026!`.

Variables (fichier `.env.local`, voir `.env.example`) : URL de la gateway et de Keycloak.

## Ecrans

| Menu | Profils | Contenu |
|---|---|---|
| Tableau de bord | tous | stock par projet, CA réservé, KPI commerciaux |
| Projets & lots | tous | stock filtrable, option / réservation (commercial), structure + prix (admin SI) |
| Grilles de prix | admin SI, direction | demandes de modification, validation direction |
| Prospects, Agenda & relances | commerciaux | CRM complet, comptes rendus < 24 h |
| Réservations → **dossier** | tous | contrôle ADV, pièces (upload), compromis, échéancier, encaissements, désistement, changement de lot, notaire |
| Validations | ADV, resp. commercial, DAF, direction | remises, désistements, changements de lot |
| Trésorerie | comptable, DAF | encaissements + reçus, remises en banque, impayés, relances, crédits, corrections, export Sage |
| Notaire, livraison, SAV | ADV, SAV, direction | dossiers notaire, actes, planning, PV & clés, tickets |
| Administration | admin SI | utilisateurs (Keycloak), journal d'activité |

## Production

```bash
docker build -t gecimmo-frontend --build-arg VITE_API_URL=https://api.exemple.ma .
```
