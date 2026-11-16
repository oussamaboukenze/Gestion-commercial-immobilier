import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../api/models.dart';
import '../format.dart';
import '../widgets/communs.dart';

class _Agenda {
  final List<RendezVous> rdv;
  final List<RendezVous> enRetard;
  final List<RelanceCrm> relances;
  _Agenda(this.rdv, this.enRetard, this.relances);
}

/// CRM > Rendez-vous & visites + relances du jour.
class AgendaScreen extends StatefulWidget {
  const AgendaScreen({super.key});

  @override
  State<AgendaScreen> createState() => _AgendaScreenState();
}

class _AgendaScreenState extends State<AgendaScreen> {
  late Future<_Agenda> _future = _charger();

  Future<_Agenda> _charger() async {
    final maintenant = DateTime.now();
    final r = await Future.wait([
      Api.get('/api/crm/rendez-vous', {
        'du': isoDate(maintenant.subtract(const Duration(days: 7))),
        'au': isoDate(maintenant.add(const Duration(days: 14))),
      }),
      Api.get('/api/crm/rendez-vous/comptes-rendus-en-retard'),
      Api.get('/api/crm/relances/du-jour'),
    ]);
    return _Agenda(
      Api.contenu(r[0]).map(RendezVous.fromJson).toList(),
      Api.contenu(r[1]).map(RendezVous.fromJson).toList(),
      Api.contenu(r[2]).map(RelanceCrm.fromJson).toList(),
    );
  }

  Future<void> _rafraichir() async {
    setState(() => _future = _charger());
    await _future;
  }

  Future<void> _compteRendu(RendezVous r) async {
    final texte = await saisirTexte(context, 'Compte rendu — ${r.prospectNom}', label: 'Intérêt, objections, suite à donner…');
    if (texte == null || !mounted) return;
    final ok = await executer(context, () => Api.post('/api/crm/rendez-vous/${r.id}/compte-rendu', {'compteRendu': texte}),
        'Compte rendu enregistré');
    if (ok) _rafraichir();
  }

  Future<void> _relanceFaite(RelanceCrm r) async {
    final resultat = await saisirTexte(context, 'Résultat de la relance', label: 'Ex. rappeler lundi');
    if (!mounted) return;
    final ok = await executer(context, () => Api.post('/api/crm/relances/${r.id}/faite', {'resultat': resultat}), 'Relance faite');
    if (ok) _rafraichir();
  }

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: _rafraichir,
      child: Chargement<_Agenda>(
        future: _future,
        onReessayer: _rafraichir,
        builder: (a) => ListView(padding: const EdgeInsets.all(12), children: [
          if (a.enRetard.isNotEmpty)
            Card(
              color: Colors.orange.shade50,
              child: Column(children: [
                const ListTile(
                  leading: Icon(Icons.warning_amber, color: Colors.orange),
                  title: Text('Comptes rendus en retard (> 24 h)'),
                ),
                ...a.enRetard.map((r) => ListTile(
                      title: Text(r.prospectNom),
                      subtitle: Text(dateHeure(r.dateHeure)),
                      trailing: TextButton(onPressed: () => _compteRendu(r), child: const Text('Saisir')),
                    )),
              ]),
            ),
          Text('Relances du jour', style: Theme.of(context).textTheme.titleMedium),
          if (a.relances.isEmpty) const Padding(padding: EdgeInsets.all(8), child: Text('Aucune relance')),
          ...a.relances.map((r) => Card(
                child: ListTile(
                  leading: const Icon(Icons.phone_in_talk),
                  title: Text(r.prospectNom),
                  subtitle: Text('${r.objet}\n${r.telephone}'),
                  isThreeLine: true,
                  trailing: IconButton(icon: const Icon(Icons.check), onPressed: () => _relanceFaite(r)),
                ),
              )),
          const SizedBox(height: 12),
          Text('Rendez-vous', style: Theme.of(context).textTheme.titleMedium),
          if (a.rdv.isEmpty) const Padding(padding: EdgeInsets.all(8), child: Text('Aucun rendez-vous')),
          ...a.rdv.map((r) {
            final passe = DateTime.tryParse(r.dateHeure)?.isBefore(DateTime.now()) ?? false;
            return Card(
              child: ListTile(
                leading: Icon(r.type == 'VISITE' ? Icons.directions_walk : r.type == 'APPEL' ? Icons.call : Icons.business),
                title: Text(r.prospectNom),
                subtitle: Text('${dateHeure(r.dateHeure)} · ${libelle(r.type)}'),
                trailing: r.statut == 'PLANIFIE' && passe
                    ? TextButton(onPressed: () => _compteRendu(r), child: const Text('Compte rendu'))
                    : StatutChip(r.statut),
              ),
            );
          }),
        ]),
      ),
    );
  }
}
