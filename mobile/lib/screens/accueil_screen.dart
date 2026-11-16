import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../api/auth.dart';
import '../api/models.dart';
import '../format.dart';
import '../widgets/communs.dart';

class _Donnees {
  final num caReserve;
  final Map<String, dynamic> indicateurs;
  final List<RelanceCrm> relances;
  final int comptesRendusEnRetard;
  _Donnees(this.caReserve, this.indicateurs, this.relances, this.comptesRendusEnRetard);
}

/// Mes chiffres du mois + tâches du jour.
class AccueilScreen extends StatefulWidget {
  final void Function(int index) onAller;
  const AccueilScreen({super.key, required this.onAller});

  @override
  State<AccueilScreen> createState() => _AccueilScreenState();
}

class _AccueilScreenState extends State<AccueilScreen> {
  late Future<_Donnees> _future = _charger();

  Future<_Donnees> _charger() async {
    final resultats = await Future.wait([
      Api.get('/api/ventes/reporting/mes-chiffres'),
      Api.get('/api/crm/indicateurs/moi'),
      Api.get('/api/crm/relances/du-jour'),
      Api.get('/api/crm/rendez-vous/comptes-rendus-en-retard'),
    ]);
    return _Donnees(
      (resultats[0] as Map)['caReserve'] as num? ?? 0,
      Map<String, dynamic>.from(resultats[1] as Map),
      Api.contenu(resultats[2]).map(RelanceCrm.fromJson).toList(),
      (resultats[3] as List).length,
    );
  }

  Future<void> _rafraichir() async {
    setState(() => _future = _charger());
    await _future;
  }

  Widget _kpi(String titre, String valeur, IconData icone) => Expanded(
        child: Card(
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Icon(icone, color: Theme.of(context).colorScheme.primary),
              const SizedBox(height: 8),
              Text(valeur, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
              Text(titre, style: const TextStyle(fontSize: 12, color: Colors.grey)),
            ]),
          ),
        ),
      );

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: _rafraichir,
      child: Chargement<_Donnees>(
        future: _future,
        onReessayer: _rafraichir,
        builder: (d) => ListView(padding: const EdgeInsets.all(16), children: [
          Text('Bonjour ${Session.instance.nom}', style: Theme.of(context).textTheme.titleLarge),
          Text('Projets : ${Session.instance.projets.join(', ')}', style: const TextStyle(color: Colors.grey)),
          const SizedBox(height: 16),
          Card(
            color: Theme.of(context).colorScheme.primaryContainer,
            child: ListTile(
              leading: const Icon(Icons.trending_up),
              title: const Text('CA réservé du mois'),
              subtitle: Text(dh(d.caReserve), style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
            ),
          ),
          Row(children: [
            _kpi('Visites', '${d.indicateurs['visitesEffectuees'] ?? 0}', Icons.directions_walk),
            _kpi('Transfo.', '${d.indicateurs['tauxTransformation'] ?? 0} %', Icons.percent),
            _kpi('Réservations', '${d.indicateurs['prospectsConvertis'] ?? 0}', Icons.handshake),
          ]),
          if (d.comptesRendusEnRetard > 0)
            Card(
              color: Colors.orange.shade50,
              child: ListTile(
                leading: const Icon(Icons.warning_amber, color: Colors.orange),
                title: Text('${d.comptesRendusEnRetard} compte(s) rendu(s) en retard'),
                subtitle: const Text('Obligatoire sous 24 h après la visite'),
                onTap: () => widget.onAller(3),
              ),
            ),
          const SizedBox(height: 8),
          Text('Mes relances du jour (${d.relances.length})', style: Theme.of(context).textTheme.titleMedium),
          if (d.relances.isEmpty) const Padding(padding: EdgeInsets.all(8), child: Text('Rien à relancer aujourd\'hui 👍')),
          ...d.relances.map((r) => Card(
                child: ListTile(
                  leading: const Icon(Icons.phone_in_talk),
                  title: Text(r.prospectNom),
                  subtitle: Text('${r.objet} · ${r.telephone}'),
                  trailing: Text(date(r.dateRelance)),
                  onTap: () => widget.onAller(3),
                ),
              )),
        ]),
      ),
    );
  }
}
