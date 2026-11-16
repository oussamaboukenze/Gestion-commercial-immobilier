import 'package:flutter/material.dart';

import '../format.dart';

/// Couleur d'un statut (lot, réservation, prospect...).
Color couleurStatut(String statut) {
  switch (statut) {
    case 'DISPONIBLE':
    case 'CONVERTI':
    case 'PAYEE':
      return Colors.green;
    case 'OPTION':
    case 'EN_ATTENTE_VALIDATION_ADV':
    case 'EN_COURS':
    case 'PLANIFIE':
      return Colors.orange;
    case 'RESERVE':
    case 'VALIDEE':
    case 'NOUVEAU':
      return Colors.blue;
    case 'COMPROMIS_SIGNE':
      return Colors.indigo;
    case 'REJETEE':
    case 'DESISTEE':
    case 'EN_RETARD':
      return Colors.red;
    default:
      return Colors.grey;
  }
}

class StatutChip extends StatelessWidget {
  final String statut;
  const StatutChip(this.statut, {super.key});

  @override
  Widget build(BuildContext context) {
    final c = couleurStatut(statut);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(border: Border.all(color: c), borderRadius: BorderRadius.circular(12)),
      child: Text(libelle(statut), style: TextStyle(color: c, fontSize: 12)),
    );
  }
}

void afficherMessage(BuildContext context, String message, {bool erreur = false}) {
  ScaffoldMessenger.of(context).showSnackBar(SnackBar(
    content: Text(message),
    backgroundColor: erreur ? Colors.red.shade700 : null,
  ));
}

/// Exécute une action, affiche le succès ou l'erreur métier. Renvoie true si OK.
Future<bool> executer(BuildContext context, Future<dynamic> Function() action, String succes) async {
  try {
    await action();
    if (context.mounted) afficherMessage(context, succes);
    return true;
  } catch (e) {
    if (context.mounted) afficherMessage(context, e.toString().replaceFirst('Exception: ', ''), erreur: true);
    return false;
  }
}

/// Affiche une Future : chargement, erreur (avec "Réessayer") ou contenu.
class Chargement<T> extends StatelessWidget {
  final Future<T> future;
  final Widget Function(T data) builder;
  final VoidCallback onReessayer;
  const Chargement({super.key, required this.future, required this.builder, required this.onReessayer});

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<T>(
      future: future,
      builder: (context, snap) {
        if (snap.connectionState != ConnectionState.done) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snap.hasError) {
          return Center(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Column(mainAxisSize: MainAxisSize.min, children: [
                const Icon(Icons.cloud_off, size: 48, color: Colors.grey),
                const SizedBox(height: 12),
                Text(snap.error.toString(), textAlign: TextAlign.center),
                const SizedBox(height: 12),
                FilledButton(onPressed: onReessayer, child: const Text('Réessayer')),
              ]),
            ),
          );
        }
        return builder(snap.data as T);
      },
    );
  }
}

class Vide extends StatelessWidget {
  final String texte;
  const Vide(this.texte, {super.key});

  @override
  Widget build(BuildContext context) => ListView(children: [
        const SizedBox(height: 80),
        Center(child: Text(texte, style: const TextStyle(color: Colors.grey))),
      ]);
}

/// Dialogue de saisie d'un texte (motif, compte rendu...).
Future<String?> saisirTexte(BuildContext context, String titre, {String label = '', bool multiligne = true}) {
  final ctrl = TextEditingController();
  return showDialog<String>(
    context: context,
    builder: (ctx) => AlertDialog(
      title: Text(titre),
      content: TextField(
        controller: ctrl,
        autofocus: true,
        maxLines: multiligne ? 4 : 1,
        decoration: InputDecoration(labelText: label, border: const OutlineInputBorder()),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
        FilledButton(
          onPressed: () => Navigator.pop(ctx, ctrl.text.trim().isEmpty ? null : ctrl.text.trim()),
          child: const Text('Valider'),
        ),
      ],
    ),
  );
}
