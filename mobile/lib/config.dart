/// Adresses du backend.
/// Emulateur Android : 10.0.2.2 = "localhost" du PC.
/// Téléphone réel : remplacer par l'IP du PC (ex. --dart-define=HOST=192.168.1.20).
class Config {
  static const String host = String.fromEnvironment('HOST', defaultValue: '10.0.2.2');
  static const String apiUrl = String.fromEnvironment('API_URL', defaultValue: 'http://$host:8080');
  static const String keycloakUrl = String.fromEnvironment('KEYCLOAK_URL', defaultValue: 'http://$host:8180');
  static const String realm = 'ahi';
  static const String clientId = 'gecimmo-mobile';
}
