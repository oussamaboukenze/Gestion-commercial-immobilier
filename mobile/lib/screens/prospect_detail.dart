import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../api/models.dart';
import '../format.dart';
import '../widgets/communs.dart';
import 'prospect_form.dart';

/// Fiche prospect : rendez-vous, relances, actions.
class ProspectDetail extends StatefulWidget {
  final Prospect prospect;
  const ProspectDetail({super.key, required this.prospect});

  @override
  State<ProspectDetail> createState() => _ProspectDetailState();
}

class _ProspectDetailState extends State<ProspectDetail> {
  late Future<(List<RendezVous>, List<Map<String, dynamic>>)> _future = _charger();

  Future<(List<RendezVous>, List<Map<String, dynamic>>)> _charger() async {
    final id = widget.prospect.id;
    final r = await Future.wait([
      Api.get('/api/crm/prospects/$id/rendez-vous'),
      Api.get('/api/crm/prospects/$id/relances'),
    ]);
    return (Api.contenu(r[0]).map(RendezVous.fromJson).toList(), Api.contenu(r[1]));
  }

  void _rafraichir() => setState(() => _future = _charger());

  Future<DateTime?> _choisirDateHeure() async {
    final d = await showDatePicker(
        context: context, firstDate: DateTime.now().subtract(const Duration(days: 1)),
        lastDate: DateTime.now().add(const Duration(days: 365)), initialDate: DateTime.now());
    if (d == null || !mounted) return null;
    final h = await showTimePicker(context: context, initialTime: const TimeOfDay(hour: 10, minute: 0));
    if (h == null) return null;
    return DateTime(d.year, d.month, d.day, h.hour, h.minute);
  }

  Future<void> _planifier() async {
    final type = await showDialog<String>(
      context: context,
      builder: (ctx) => SimpleDialog(title: const Text('Type de rendez-vous'), children: [
        for (final t in ['VISITE', 'RDV_BUREAU', 'APPEL'])
          SimpleDialogOption(onPressed: () => Navigator.pop(ctx, t), child: Text(libelle(t))),
      ]),
    );
    if (type == null) return;
    final quand = await _choisirDateHeure();
    if (quand == null || !mounted) return;
    final ok = await executer(context,
        () => Api.post('/api/crm/rendez-vous', {'prospectId': widget.prospect.id, 'dateHeure': isoDateHeure(quand), 'type': type}),
        'Rendez-vous planifié');
    if (ok) _rafraichir();
  }

  Future<void> _relance() async {
    final d = await showDatePicker(
        context: context, firstDate: DateTime.now(), lastDate: DateTime.now().add(const Duration(days: 365)),
        initialDate: DateTime.now().add(const Duration(days: 2)));
    if (d == null || !mounted) return;
    final objet = await saisirTexte(context, 'Objet de la relance', multiligne: false);
    if (objet == null || !mounted) return;
    final ok = await executer(context,
        () => Api.post('/api/crm/relances', {'prospectId': widget.prospect.id, 'dateRelance': isoDate(d), 'objet': objet}),
        'Relance programmée');
    if (ok) _rafraichir();
  }

  Future<void> _perdu() async {
    final motif = await saisirTexte(context, 'Motif de perte', label: 'Ex. budget insuffisant');
    if (motif == null || !mounted) return;
    final ok = await executer(context, () => Api.post('/api/crm/prospects/${widget.prospect.id}/perdu', {'motif': motif}),
        'Prospect marqué perdu');
    if (ok && mounted) Navigator.pop(context);
  }

  @override
  Widget build(BuildContext context) {
    final p = widget.prospect;
    final actif = p.statut != 'CONVERTI' && p.statut != 'PERDU';
    return Scaffold(
      appBar: AppBar(title: Text(p.nomComplet), actions: [
        IconButton(
          icon: const Icon(Icons.edit),
          onPressed: () async {
            final ok = await Navigator.push<bool>(context, MaterialPageRoute(builder: (_) => ProspectForm(prospect: p)));
            if (ok == true && context.mounted) Navigator.pop(context);
          },
        ),
      ]),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        Row(children: [StatutChip(p.statut), const SizedBox(width: 8), Text('Projet ${p.projetCode}')]),
        const SizedBox(height: 12),
        ListTile(contentPadding: EdgeInsets.zero, leading: const Icon(Icons.phone), title: Text(p.telephone)),
        ListTile(contentPadding: EdgeInsets.zero, leading: const Icon(Icons.badge), title: Text('CIN ${p.cin}')),
        if (p.email != null && p.email!.isNotEmpty)
          ListTile(contentPadding: EdgeInsets.zero, leading: const Icon(Icons.email), title: Text(p.email!)),
        ListTile(
            contentPadding: EdgeInsets.zero,
            leading: const Icon(Icons.savings),
            title: Text('${dh(p.budget)} · ${p.typologieRecherchee}'),
            subtitle: Text('Source : ${libelle(p.source)}')),
        if (actif)
          Wrap(spacing: 8, children: [
            FilledButton.icon(onPressed: _planifier, icon: const Icon(Icons.event), label: const Text('Rendez-vous')),
            OutlinedButton.icon(onPressed: _relance, icon: const Icon(Icons.alarm), label: const Text('Relance')),
            TextButton(onPressed: _perdu, child: const Text('Perdu', style: TextStyle(color: Colors.red))),
          ]),
        const Divider(height: 32),
        FutureBuilder(
          future: _future,
          builder: (context, snap) {
            if (!snap.hasData) {
              return snap.hasError ? Text(snap.error.toString()) : const Center(child: CircularProgressIndicator());
            }
            final (rdvs, relances) = snap.data!;
            return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text('Rendez-vous (${rdvs.length})', style: Theme.of(context).textTheme.titleMedium),
              ...rdvs.map((r) => ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: const Icon(Icons.event_note),
                    title: Text('${dateHeure(r.dateHeure)} · ${libelle(r.type)}'),
                    subtitle: r.compteRendu == null ? null : Text(r.compteRendu!),
                    trailing: StatutChip(r.statut),
                  )),
              const SizedBox(height: 16),
              Text('Relances (${relances.length})', style: Theme.of(context).textTheme.titleMedium),
              ...relances.map((r) => ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: Icon(r['faite'] == true ? Icons.check_circle : Icons.alarm,
                        color: r['faite'] == true ? Colors.green : null),
                    title: Text(r['objet'].toString()),
                    subtitle: Text(date(r['dateRelance']?.toString())),
                  )),
            ]);
          },
        ),
      ]),
    );
  }
}
