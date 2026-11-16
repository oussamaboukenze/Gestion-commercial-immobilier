import 'dart:convert';

import 'package:http/http.dart' as http;

import '../config.dart';

/// Session Keycloak de l'utilisateur connecté (token gardé en mémoire uniquement).
class Session {
  Session._();
  static final Session instance = Session._();

  String? _accessToken;
  String? _refreshToken;
  DateTime _expiration = DateTime.fromMillisecondsSinceEpoch(0);
  String username = '';
  String nom = '';
  List<String> roles = [];
  List<String> projets = [];

  bool get connecte => _accessToken != null;
  bool get estCommercial => roles.contains('COMMERCIAL') || roles.contains('RESP_COMMERCIAL');

  Uri get _tokenUrl => Uri.parse('${Config.keycloakUrl}/realms/${Config.realm}/protocol/openid-connect/token');

  /// Connexion par identifiant / mot de passe (client public "gecimmo-mobile").
  Future<void> connecter(String identifiant, String motDePasse) async {
    final resp = await http.post(_tokenUrl, body: {
      'grant_type': 'password',
      'client_id': Config.clientId,
      'username': identifiant,
      'password': motDePasse,
      'scope': 'openid',
    }).timeout(const Duration(seconds: 15));
    if (resp.statusCode != 200) {
      String message = 'Identifiant ou mot de passe incorrect';
      try {
        final erreur = jsonDecode(resp.body)['error_description'];
        if (erreur is String && erreur.contains('not fully set up')) {
          message = 'Compte à finaliser : connectez-vous une première fois sur le site web.';
        }
      } catch (_) {}
      throw Exception(message);
    }
    _lireTokens(jsonDecode(resp.body) as Map<String, dynamic>);
  }

  /// Token valide (rafraîchi s'il expire dans moins de 30 s).
  Future<String> token() async {
    if (_accessToken == null) throw Exception('Non connecté');
    if (DateTime.now().isAfter(_expiration.subtract(const Duration(seconds: 30)))) {
      final resp = await http.post(_tokenUrl, body: {
        'grant_type': 'refresh_token',
        'client_id': Config.clientId,
        'refresh_token': _refreshToken ?? '',
      });
      if (resp.statusCode != 200) {
        deconnecter();
        throw Exception('Session expirée, reconnectez-vous.');
      }
      _lireTokens(jsonDecode(resp.body) as Map<String, dynamic>);
    }
    return _accessToken!;
  }

  void deconnecter() {
    _accessToken = null;
    _refreshToken = null;
    roles = [];
    projets = [];
  }

  void _lireTokens(Map<String, dynamic> json) {
    _accessToken = json['access_token'] as String;
    _refreshToken = json['refresh_token'] as String?;
    _expiration = DateTime.now().add(Duration(seconds: (json['expires_in'] as num).toInt()));
    final claims = _decoderJwt(_accessToken!);
    username = (claims['preferred_username'] ?? '').toString();
    nom = (claims['name'] ?? username).toString();
    final realmAccess = claims['realm_access'];
    roles = realmAccess is Map && realmAccess['roles'] is List
        ? List<String>.from(realmAccess['roles'] as List)
        : <String>[];
    final groups = claims['groups'] is List ? List<String>.from(claims['groups'] as List) : <String>[];
    projets = groups
        .map((g) => g.startsWith('/') ? g.substring(1) : g)
        .where((g) => g.startsWith('projet-'))
        .map((g) => g.substring('projet-'.length))
        .toList();
  }

  static Map<String, dynamic> _decoderJwt(String jwt) {
    final parts = jwt.split('.');
    if (parts.length < 2) return {};
    final payload = utf8.decode(base64Url.decode(base64Url.normalize(parts[1])));
    return jsonDecode(payload) as Map<String, dynamic>;
  }
}
