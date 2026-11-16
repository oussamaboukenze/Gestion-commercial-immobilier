import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:http/http.dart' as http;

import '../config.dart';
import 'auth.dart';

/// Erreur métier renvoyée par le backend (ProblemDetail : champ "detail").
class ApiException implements Exception {
  final int status;
  final String message;
  ApiException(this.status, this.message);
  @override
  String toString() => message;
}

/// Appels REST vers la gateway GECIMMO avec le token Keycloak.
class Api {
  static const _timeout = Duration(seconds: 20);

  static Future<dynamic> get(String chemin, [Map<String, String?> params = const {}]) async {
    final query = Map<String, String>.fromEntries(
        params.entries.where((e) => e.value != null && e.value!.isNotEmpty).map((e) => MapEntry(e.key, e.value!)));
    final uri = Uri.parse(Config.apiUrl + chemin).replace(queryParameters: query.isEmpty ? null : query);
    return _envoyer(() async => http.get(uri, headers: await _entetes()));
  }

  static Future<dynamic> post(String chemin, [Map<String, dynamic>? corps]) async {
    final uri = Uri.parse(Config.apiUrl + chemin);
    return _envoyer(() async => http.post(uri, headers: await _entetes(), body: jsonEncode(corps ?? {})));
  }

  static Future<dynamic> put(String chemin, [Map<String, dynamic>? corps]) async {
    final uri = Uri.parse(Config.apiUrl + chemin);
    return _envoyer(() async => http.put(uri, headers: await _entetes(), body: jsonEncode(corps ?? {})));
  }

  static Future<Map<String, String>> _entetes() async => {
        'Authorization': 'Bearer ${await Session.instance.token()}',
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      };

  static Future<dynamic> _envoyer(Future<http.Response> Function() requete) async {
    http.Response resp;
    try {
      resp = await requete().timeout(_timeout);
    } on SocketException {
      throw ApiException(0, 'Serveur injoignable : vérifiez le réseau et l\'adresse du serveur.');
    } on TimeoutException {
      throw ApiException(0, 'Le serveur met trop de temps à répondre.');
    }
    final texte = utf8.decode(resp.bodyBytes);
    if (resp.statusCode >= 200 && resp.statusCode < 300) {
      return texte.isEmpty ? null : jsonDecode(texte);
    }
    String message = 'Erreur ${resp.statusCode}';
    try {
      final json = jsonDecode(texte);
      if (json is Map && json['detail'] != null) message = json['detail'].toString();
      if (json is Map && json['erreurs'] is Map) {
        message += ' : ${(json['erreurs'] as Map).entries.map((e) => '${e.key} ${e.value}').join(', ')}';
      }
    } catch (_) {
      if (resp.statusCode == 401) message = 'Session expirée, reconnectez-vous.';
      if (resp.statusCode == 403) message = 'Action hors de votre périmètre.';
      if (resp.statusCode == 503) message = 'Service indisponible, réessayez dans un instant.';
    }
    throw ApiException(resp.statusCode, message);
  }

  /// Contenu d'une page Spring Data ({ content: [...] }).
  static List<Map<String, dynamic>> contenu(dynamic page) {
    if (page is Map && page['content'] is List) {
      return List<Map<String, dynamic>>.from(page['content'] as List);
    }
    if (page is List) return List<Map<String, dynamic>>.from(page);
    return [];
  }
}
