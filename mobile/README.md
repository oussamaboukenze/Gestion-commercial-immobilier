# GECIMMO — Application mobile Flutter (commerciaux)

Application des commerciaux AHI sur le terrain. Elle utilise la même API que le back-office (gateway :8080)
et le même login Keycloak (client `gecimmo-mobile`). Réservée aux profils COMMERCIAL et RESP_COMMERCIAL.

| Onglet | Fonctions |
|---|---|
| Accueil | CA réservé du mois, visites, taux de transformation, relances du jour, alertes compte rendu > 24 h |
| Stock | lots par projet / typologie / statut, **option 48 h**, **réservation** |
| Prospects | recherche, création, fiche (rendez-vous, relances, perdu) |
| Agenda | visites, comptes rendus, relances du jour |
| Ventes | mes options (réserver / lever), mes réservations et leur statut |

## Première installation

Le dossier contient le code Dart (`lib/`) ; les dossiers Android / iOS se génèrent avec Flutter :

```bash
cd mobile
flutter create . --project-name gecimmo_mobile --org ma.ahi --platforms android,ios
flutter pub get
```

`flutter create .` ne remplace pas les fichiers existants (`lib/`, `pubspec.yaml`, `test/`).

### Autoriser HTTP en développement (Android)

Le backend local est en `http://` : dans `android/app/src/main/AndroidManifest.xml`, ajouter la permission
Internet et `usesCleartextTraffic` :

```xml
<manifest ...>
    <uses-permission android:name="android.permission.INTERNET"/>
    <application
        android:usesCleartextTraffic="true"
        ...>
```

## Lancer

Backend démarré (voir `../backend/README.md`), puis :

```bash
flutter run                                   # émulateur Android : le PC = 10.0.2.2
flutter run --dart-define=HOST=192.168.1.20   # téléphone réel : IP du PC sur le Wi-Fi
```

Connexion : `commercial1` / `Gecimmo2026!` (projets RNA et JBK).

> Le login utilise le "password grant" de Keycloak (simple pour un projet d'étude). En production : login par
> navigateur (Authorization Code + PKCE, package `flutter_appauth`).
