# GECIMMO — Gestion commerciale immobilière

Projet d'étude : ERP de gestion commerciale pour un promoteur immobilier fictif,
**Atlas Horizon Immobilier** (Casablanca). Cycle couvert de bout en bout :
prospection → option → réservation → compromis → encaissements → acte → livraison → SAV.

| Dossier | Contenu | Techno |
|---|---|---|
| [`backend/`](backend) | 8 microservices + infrastructure Docker | Java 17, Spring Boot 3.5, Spring Cloud, PostgreSQL, RabbitMQ, Keycloak |
| [`frontend/`](frontend) | Back-office (ADV, trésorerie, direction, SAV, admin SI) | React 19, TypeScript, MUI |
| [`mobile/`](mobile) | Application des commerciaux | Flutter |
| `.github/workflows/ci.yml` | Intégration continue | GitHub Actions |

```
React (5173) ─┐                       ┌─ referentiel :8081   crm :8082
              ├─► gateway :8080 ──────┼─ vente :8083         tresorerie :8084
Flutter ──────┘   (JWT Keycloak)      └─ postvente :8085     admin :8086
                        │
                  discovery :8761 (Eureka) · PostgreSQL · RabbitMQ · Keycloak :8180
```

## Démarrage rapide

```bash
cd backend && docker compose up -d          # PostgreSQL, RabbitMQ, Keycloak
# lancer les 8 services depuis IntelliJ (voir backend/README.md)
cd ../frontend && npm install && npm run dev # http://localhost:5173
cd ../mobile && flutter create . --project-name gecimmo_mobile --org ma.ahi && flutter run
```

Utilisateurs de test : `commercial1`, `adv1`, `comptable1`, `resp1`, `daf1`, `direction1`, `admin1`, `sav1`
— mot de passe `Gecimmo2026!`.
