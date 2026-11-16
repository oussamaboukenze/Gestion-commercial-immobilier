/// Formats d'affichage (sans dépendance externe).
String dh(num? v) {
  if (v == null) return '—';
  final negatif = v < 0;
  final parties = v.abs().toStringAsFixed(2).split('.');
  final entier = parties[0];
  final buffer = StringBuffer();
  for (var i = 0; i < entier.length; i++) {
    if (i > 0 && (entier.length - i) % 3 == 0) buffer.write(' ');
    buffer.write(entier[i]);
  }
  return '${negatif ? '-' : ''}$buffer,${parties[1]} DH';
}

String deuxChiffres(int n) => n.toString().padLeft(2, '0');

String date(String? iso) {
  if (iso == null || iso.isEmpty) return '—';
  final d = DateTime.tryParse(iso);
  if (d == null) return iso;
  return '${deuxChiffres(d.day)}/${deuxChiffres(d.month)}/${d.year}';
}

String dateHeure(String? iso) {
  if (iso == null || iso.isEmpty) return '—';
  final d = DateTime.tryParse(iso);
  if (d == null) return iso;
  return '${date(iso)} ${deuxChiffres(d.hour)}:${deuxChiffres(d.minute)}';
}

/// Date ISO "2026-11-16" pour l'API.
String isoDate(DateTime d) => '${d.year}-${deuxChiffres(d.month)}-${deuxChiffres(d.day)}';

/// Date-heure ISO locale "2026-11-16T10:30:00" pour l'API.
String isoDateHeure(DateTime d) => '${isoDate(d)}T${deuxChiffres(d.hour)}:${deuxChiffres(d.minute)}:00';

String libelle(String? v) {
  if (v == null || v.isEmpty) return '—';
  final s = v.replaceAll('_', ' ').toLowerCase();
  return s[0].toUpperCase() + s.substring(1);
}
