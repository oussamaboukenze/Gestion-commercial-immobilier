# GECIMMO — Backend microservices (Atlas Horizon Immobilier)

Backend Spring Boot du logiciel de gestion commerciale immobilière GECIMMO
(projet d'étude : entreprise, projets et procédures fictifs).
Il sert deux clients : **React** (back-office ADV, trésorerie, direction) et **Flutter** (commerciaux sur le terrain).

## Architecture

```
React / Flutter
      │  (JWT Keycloak)
      ▼
gateway-service :8080 ──► discovery-service :8761 (Eureka)
      │
      ├─► referentiel-service :8081   projets, tranches, immeubles, lots, grilles de prix
      ├─► crm-service         :8082   prospects, rendez-vous, relances commerciales
      ├─► vente-service       :8083   options, réservations, compromis, échéanciers,
      │                               désistements, changements de lot
      ├─► tresorerie-service  :8084   encaissements, remises, impayés, relances clients,
      │                               crédits bancaires, export Sage 100
      ├─► postvente-service   :8085   notaire & actes, livraison (PV, clés), SAV (tickets)
      └─► admin-service       :8086   utilisateurs & droits (API Keycloak), journal d'activité

PostgreSQL (1 base par service) · RabbitMQ (événements) · Keycloak (login + rôles)
```

| Techno | Version |
|---|---|
| Java | 17 |
| Spring Boot | 3.5.6 |
| Spring Cloud | 2025.0.0 (Gateway, Eureka, OpenFeign) |
| PostgreSQL | 16 |
| RabbitMQ | 3.13 |
| Keycloak | 26 |

### Communication entre services

**Synchrone (OpenFeign, `/internal/**`)** : quand la réponse est nécessaire tout de suite.
- vente → referentiel : lire un lot, changer son statut de façon atomique (409 si un autre commercial a été plus rapide)
- vente → crm : lire un prospect
- tresorerie → vente : vérifier un dossier avant d'encaisser

**Asynchrone (RabbitMQ, exchange `gecimmo.events`)** : quand un autre service doit "être informé".

| Routing key | Émetteur | Récepteur | Effet |
|---|---|---|---|
| `lot.statut` | vente | referentiel | option expirée, compromis signé, désistement → statut du lot |
| `reservation.validee` | vente | crm | prospect → CONVERTI |
| `encaissement.enregistre` / `encaissement.annule` | tresorerie | vente | total encaissé du dossier, répartition sur les échéances |
| `echeance.retard` | vente | tresorerie | relances J+7, J+15, mise en demeure J+30 |
| `acte.signe`, `livraison.effectuee` | postvente | admin (journal) | traçabilité |
| `#` (tous) | tous | admin | journal d'activité |

Les événements sont publiés **après le commit** de la transaction, et la réception des encaissements est **idempotente**
(un message reçu deux fois n'est compté qu'une fois).

## Règles AHI implémentées

- Option : 48 h max, 1 option active par prospect, expiration automatique (planificateur toutes les 5 min)
- Un lot ne peut pas avoir deux réservations actives (verrou en base `lots_engages` + verrou optimiste `@Version`)
- Réservation : avance ≥ 5 % du prix TTC, pièces CIN / revenus / reçu, avance confirmée par la trésorerie, validation ADV
- Réservation valable 30 jours (liste "à régulariser")
- Remises : ≤ 2 % commercial, 2–5 % responsable commercial, > 5 % direction
- Compromis : versement cumulé ≥ 20 %, échéancier 20 / 30 / 50
- Désistement avant compromis : remboursement − 10 % de frais, validation responsable commercial
- Désistement après compromis : montant selon clauses, validation responsable commercial **+ DAF**
- Changement de lot : validation responsable commercial, versements conservés, échéancier recalculé
- Encaissement validé jamais supprimé : contre-passation validée par le DAF ; chèque impayé → échéance due + relance
- Plafond espèces : paramètre `gecimmo.tresorerie.plafond-especes` (à fixer selon la politique interne)
- Un utilisateur ne voit que les projets auxquels il est affecté (groupes Keycloak `projet-RNA`, `projet-JBK`...)
- Un commercial ne voit que ses propres prospects et réservations
- Acte définitif : compromis signé + solde payé (ou crédit débloqué) → lot "Vendu"
- Livraison : PV + remise des clés uniquement si solde = 0 → lot "Livré" ; SAV ouvert 12 mois
- Comptes utilisateurs désactivés, jamais supprimés ; 1 profil + projets autorisés (moindre privilège)
- Documents : bon de réservation, compromis + échéancier, reçu, PV de livraison, lettres de relance (HTML → PDF via "Imprimer")
- Pièces du dossier : upload PDF / JPEG / PNG (10 Mo), case "fournie" cochée automatiquement

## Démarrage

### 1. Infrastructure

```bash
cd backend
docker compose up -d
```

- RabbitMQ : http://localhost:15672 (gecimmo / gecimmo)
- Keycloak : http://localhost:8180 (admin / admin), realm `ahi` importé automatiquement

### 2. Microservices (IntelliJ)

Ouvrir le dossier `backend` (projet Maven multi-modules) puis lancer **dans cet ordre** :

1. `DiscoveryServiceApplication` → http://localhost:8761
2. `ReferentielServiceApplication`, `CrmServiceApplication`, `VenteServiceApplication`, `TresorerieServiceApplication`,
   `PostventeServiceApplication`, `AdminServiceApplication`
3. `GatewayServiceApplication`

En ligne de commande : `mvn clean package -DskipTests` puis `java -jar <service>/target/<service>-1.0.0-SNAPSHOT.jar`.

Tout dans Docker (seul Docker Desktop est nécessaire) : double-clic sur **`lancer.bat`** à la racine du projet,
ou `docker compose --profile apps up -d --build` (Maven compile dans Docker, voir `Dockerfile`). Arrêt : `arreter.bat`.

### 3. Tester

- Swagger de chaque service : `http://localhost:808X/swagger-ui.html`
- Scénario complet prêt à lancer : [`requests.http`](requests.http) (client HTTP d'IntelliJ)

### Utilisateurs de test (mot de passe : `Gecimmo2026!`)

| Utilisateur | Rôle | Projets |
|---|---|---|
| commercial1 | COMMERCIAL | RNA, JBK |
| commercial2 | COMMERCIAL | RNA |
| adv1 | ADV | tous |
| comptable1 | COMPTABLE | tous |
| resp1 | RESP_COMMERCIAL | tous |
| daf1 | DAF | tous |
| direction1 | DIRECTION | tous (par rôle) |
| admin1 | ADMIN_SI | tous (par rôle) |
| sav1 | SAV | tous |

Obtenir un token (Postman) : `POST http://localhost:8180/realms/ahi/protocol/openid-connect/token`
avec `grant_type=password`, `client_id=gecimmo-web`, `username`, `password`.

## Endpoints principaux (via la gateway :8080)

| Menu GECIMMO | Endpoint |
|---|---|
| Référentiel > Lots | `GET /api/referentiel/lots?projet=RNA&typologie=F3&statut=DISPONIBLE` |
| Référentiel > Grilles de prix | `POST /api/referentiel/lots/{id}/demandes-prix` → `POST /api/referentiel/demandes-prix/{id}/valider` |
| CRM > Prospects | `GET/POST /api/crm/prospects` |
| CRM > Rendez-vous | `POST /api/crm/rendez-vous`, `POST /api/crm/rendez-vous/{id}/compte-rendu` |
| CRM > Relances | `GET /api/crm/relances/du-jour` |
| Fiche lot > Mettre en option | `POST /api/ventes/options` |
| Ventes > Réservations | `POST /api/ventes/reservations`, `POST /api/ventes/reservations/{id}/valider` |
| Ventes > Compromis | `POST /api/ventes/compromis`, `POST /api/ventes/compromis/{id}/signer` |
| Ventes > Désistements | `POST /api/ventes/desistements`, `.../valider-resp-commercial`, `.../valider-daf` |
| Ventes > Changements de lot | `POST /api/ventes/changements-lot`, `.../{id}/valider` |
| Trésorerie > Encaissements | `POST /api/tresorerie/encaissements`, `GET .../{id}/recu` |
| Trésorerie > Remises en banque | `GET /api/tresorerie/remises/cheques-a-remettre`, `POST /api/tresorerie/remises` |
| Trésorerie > Impayés | `POST /api/tresorerie/impayes` |
| Trésorerie > Relances | `GET /api/tresorerie/relances`, `GET .../{id}/lettre` |
| Trésorerie > Crédits bancaires | `POST /api/tresorerie/credits`, `.../decision`, `.../deblocage` |
| Reporting > Stock | `GET /api/referentiel/projets/{code}/stock` |
| Reporting > Etats de vente | `GET /api/ventes/reporting/etat-ventes/{projet}` |
| Reporting > Exports | `GET /api/tresorerie/exports/sage?du=2026-11-01&au=2026-11-30` |
| Documents | `GET /api/ventes/reservations/{id}/bon-reservation`, `GET /api/ventes/compromis/{id}/document` |
| Pièces | `POST /api/ventes/reservations/{id}/pieces?type=CIN` (multipart `fichier`) |
| Notaire & Actes | `POST /api/postvente/notaire/dossiers`, `.../{id}/transmettre`, `.../{id}/acte` |
| Livraison | `POST /api/postvente/livraisons`, `.../{id}/pre-reception`, `.../{id}/pv`, `GET .../{id}/pv` |
| SAV | `POST /api/postvente/sav/tickets`, `.../{id}/interventions`, `.../{id}/cloturer` |
| Administration | `GET/POST /api/admin/utilisateurs`, `PUT .../{id}/droits`, `POST .../{id}/desactiver`, `GET /api/admin/journal` |

## Limites connues (V1)

- Si Keycloak a déjà été lancé avant l'ajout du client `gecimmo-admin` : `docker compose down` puis `up -d` pour réimporter le realm.
- `/internal/**` n'est pas protégé par token (non exposé par la gateway). En production : réseau privé + client credentials Keycloak.
- `ddl-auto: update` en développement ; en production : Flyway + `validate`.
- Publication d'événements "après commit" sans outbox : un crash entre le commit et l'envoi peut perdre un message.
- Les pièces du dossier sont des cases à cocher (pas encore d'upload de fichiers).
