import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../api/models.dart';
import '../format.dart';
import '../widgets/communs.dart';
import 'reservation_form.dart';

/// Référentiel > Lots : stock disponible, option 48 h, réservation.
class StockScreen extends StatefulWidget {
  const StockScreen({super.key});

  @override
  State<StockScreen> createState() => _StockScreenState();
}

class _StockScreenState extends State<StockScreen> {
  List<Map<String, dynamic>> _projets = [];
  String? _projet;
  String? _typologie;
  String? _statut = 'DISPONIBLE';
  late Future<List<Lot>> _future = _charger();

  Future<List<Lot>> _charger() async {
    if (_projets.isEmpty) {
      _projets = List<Map<String, dynamic>>.from(await Api.get('/api/referentiel/projets') as List);
      if (_projets.isNotEmpty) _projet ??= _projets.first['code'] as String;
    }
    if (_projet == null) return [];
    final page = await Api.get('/api/referentiel/lots',
        {'projet': _projet, 'typologie': _typologie, 'statut': _statut, 'size': '100', 'sort': 'code'});
    return Api.contenu(page).map(Lot.fromJson).toList();
  }

  Future<void> _rafraichir() async {
    setState(() => _future = _charger());
    await _future;
  }

  Future<void> _option(Lot lot) async {
    final prospect = await choisirProspect(context, lot.projetCode);
    if (prospect == null || !mounted) return;
    final ok = await executer(context, () => Api.post('/api/ventes/options', {'lotId': lot.id, 'prospectId': prospect.id}),
        'Lot ${lot.code} bloqué 48 h pour ${prospect.nomComplet}');
    if (ok) _rafraichir();
  }

  void _details(Lot lot) {
    showModalBottomSheet(
      context: context,
      builder: (ctx) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(lot.code, style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 8),
            Row(children: [StatutChip(lot.statut), const SizedBox(width: 8), Text('${lot.typologie} · étage ${lot.etage}')]),
            const SizedBox(height: 8),
            Text('Surface : ${lot.surface ?? '—'} m²'),
            Text('Prix TTC : ${dh(lot.prixTTC)}', style: const TextStyle(fontWeight: FontWeight.bold)),
            Text('Avance minimum (5 %) : ${dh(lot.prixTTC * 0.05)}', style: const TextStyle(color: Colors.grey)),
            const SizedBox(height: 16),
            Row(children: [
              if (lot.statut == 'DISPONIBLE')
                Expanded(
                  child: OutlinedButton.icon(
                    icon: const Icon(Icons.lock_clock),
                    label: const Text('Option 48 h'),
                    onPressed: () {
                      Navigator.pop(ctx);
                      _option(lot);
                    },
                  ),
                ),
              const SizedBox(width: 12),
              if (lot.statut == 'DISPONIBLE' || lot.statut == 'OPTION')
                Expanded(
                  child: FilledButton.icon(
                    icon: const Icon(Icons.assignment_turned_in),
                    label: const Text('Réserver'),
                    onPressed: () async {
                      Navigator.pop(ctx);
                      final ok = await Navigator.push<bool>(
                          context, MaterialPageRoute(builder: (_) => ReservationForm(lot: lot)));
                      if (ok == true) _rafraichir();
                    },
                  ),
                ),
            ]),
          ]),
        ),
      ),
    );
  }

  Widget _filtre<T>(String label, T? valeur, List<DropdownMenuItem<T>> items, void Function(T?) onChange) =>
      Expanded(
        child: DropdownButtonFormField<T>(
          value: valeur,
          isExpanded: true,
          decoration: InputDecoration(labelText: label, border: const OutlineInputBorder(), isDense: true),
          items: items,
          onChanged: (v) {
            onChange(v);
            _rafraichir();
          },
        ),
      );

  @override
  Widget build(BuildContext context) {
    return Column(children: [
      Padding(
        padding: const EdgeInsets.fromLTRB(12, 12, 12, 0),
        child: Row(children: [
          _filtre<String>('Projet', _projet,
              _projets.map((p) => DropdownMenuItem(value: p['code'] as String, child: Text(p['code'] as String))).toList(),
              (v) => _projet = v),
          const SizedBox(width: 8),
          _filtre<String?>('Typologie', _typologie, [
            const DropdownMenuItem<String?>(value: null, child: Text('Toutes')),
            ...typologies.map((t) => DropdownMenuItem<String?>(value: t, child: Text(t))),
          ], (v) => _typologie = v),
          const SizedBox(width: 8),
          _filtre<String?>('Statut', _statut, [
            const DropdownMenuItem<String?>(value: null, child: Text('Tous')),
            ...['DISPONIBLE', 'OPTION', 'RESERVE', 'COMPROMIS_SIGNE', 'VENDU']
                .map((s) => DropdownMenuItem<String?>(value: s, child: Text(libelle(s)))),
          ], (v) => _statut = v),
        ]),
      ),
      Expanded(
        child: RefreshIndicator(
          onRefresh: _rafraichir,
          child: Chargement<List<Lot>>(
            future: _future,
            onReessayer: _rafraichir,
            builder: (lots) => lots.isEmpty
                ? const Vide('Aucun lot pour ces critères')
                : ListView.separated(
                    itemCount: lots.length,
                    separatorBuilder: (_, __) => const Divider(height: 1),
                    itemBuilder: (_, i) {
                      final l = lots[i];
                      return ListTile(
                        title: Text(l.code, style: const TextStyle(fontWeight: FontWeight.w600)),
                        subtitle: Text('${l.typologie} · ${l.surface ?? '—'} m² · étage ${l.etage}'),
                        trailing: Column(mainAxisAlignment: MainAxisAlignment.center, crossAxisAlignment: CrossAxisAlignment.end, children: [
                          Text(dh(l.prixTTC), style: const TextStyle(fontWeight: FontWeight.bold)),
                          const SizedBox(height: 4),
                          StatutChip(l.statut),
                        ]),
                        onTap: () => _details(l),
                      );
                    },
                  ),
          ),
        ),
      ),
    ]);
  }
}

/// Sélection d'un de mes prospects actifs du projet.
Future<Prospect?> choisirProspect(BuildContext context, String projet) async {
  List<Prospect> prospects;
  try {
    final page = await Api.get('/api/crm/prospects', {'projet': projet, 'size': '200'});
    prospects = Api.contenu(page).map(Prospect.fromJson).where((p) => p.statut != 'PERDU').toList();
  } catch (e) {
    if (context.mounted) afficherMessage(context, e.toString(), erreur: true);
    return null;
  }
  if (!context.mounted) return null;
  if (prospects.isEmpty) {
    afficherMessage(context, 'Aucun prospect sur le projet $projet : créez-le d\'abord.', erreur: true);
    return null;
  }
  return showDialog<Prospect>(
    context: context,
    builder: (ctx) => SimpleDialog(
      title: const Text('Choisir le prospect'),
      children: prospects
          .map((p) => SimpleDialogOption(
                onPressed: () => Navigator.pop(ctx, p),
                child: ListTile(title: Text(p.nomComplet), subtitle: Text('${p.cin} · ${p.telephone}')),
              ))
          .toList(),
    ),
  );
}
