import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../api/models.dart';
import '../format.dart';
import '../widgets/communs.dart';
import 'reservation_form.dart';

/// Mes options (48 h) et mes réservations.
class VentesScreen extends StatefulWidget {
  const VentesScreen({super.key});

  @override
  State<VentesScreen> createState() => _VentesScreenState();
}

class _VentesScreenState extends State<VentesScreen> {
  late Future<(List<OptionLot>, List<Reservation>)> _future = _charger();

  Future<(List<OptionLot>, List<Reservation>)> _charger() async {
    final r = await Future.wait([
      Api.get('/api/ventes/options/mes-options'),
      Api.get('/api/ventes/reservations', {'size': '50', 'sort': 'dateCreation,desc'}),
    ]);
    return (Api.contenu(r[0]).map(OptionLot.fromJson).toList(), Api.contenu(r[1]).map(Reservation.fromJson).toList());
  }

  Future<void> _rafraichir() async {
    setState(() => _future = _charger());
    await _future;
  }

  String _restant(String fin) {
    final d = DateTime.tryParse(fin);
    if (d == null) return '';
    final h = d.difference(DateTime.now()).inHours;
    return h <= 0 ? 'expire bientôt' : 'encore $h h';
  }

  Future<void> _reserver(OptionLot o) async {
    try {
      final lot = Lot.fromJson(Map<String, dynamic>.from(await Api.get('/api/referentiel/lots/${o.lotId}') as Map));
      final prospect =
          Prospect.fromJson(Map<String, dynamic>.from(await Api.get('/api/crm/prospects/${o.prospectId}') as Map));
      if (!mounted) return;
      final ok = await Navigator.push<bool>(
          context, MaterialPageRoute(builder: (_) => ReservationForm(lot: lot, prospect: prospect)));
      if (ok == true) _rafraichir();
    } catch (e) {
      if (mounted) afficherMessage(context, e.toString(), erreur: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: _rafraichir,
      child: Chargement<(List<OptionLot>, List<Reservation>)>(
        future: _future,
        onReessayer: _rafraichir,
        builder: (d) {
          final (options, reservations) = d;
          return ListView(padding: const EdgeInsets.all(12), children: [
            Text('Mes options (${options.length})', style: Theme.of(context).textTheme.titleMedium),
            if (options.isEmpty) const Padding(padding: EdgeInsets.all(8), child: Text('Aucune option active')),
            ...options.map((o) => Card(
                  child: ListTile(
                    leading: const Icon(Icons.lock_clock, color: Colors.orange),
                    title: Text(o.lotCode),
                    subtitle: Text('Expire le ${dateHeure(o.dateExpiration)} (${_restant(o.dateExpiration)})'),
                    trailing: PopupMenuButton<String>(
                      onSelected: (v) async {
                        if (v == 'reserver') {
                          _reserver(o);
                        } else {
                          final ok = await executer(context, () => Api.post('/api/ventes/options/${o.id}/lever'), 'Option levée');
                          if (ok) _rafraichir();
                        }
                      },
                      itemBuilder: (_) => const [
                        PopupMenuItem(value: 'reserver', child: Text('Réserver')),
                        PopupMenuItem(value: 'lever', child: Text('Lever l\'option')),
                      ],
                    ),
                  ),
                )),
            const SizedBox(height: 12),
            Text('Mes réservations', style: Theme.of(context).textTheme.titleMedium),
            if (reservations.isEmpty) const Padding(padding: EdgeInsets.all(8), child: Text('Aucune réservation')),
            ...reservations.map((r) => Card(
                  child: ListTile(
                    title: Text('${r.numero} · ${r.clientNom}'),
                    subtitle: Text('${r.lotCode}\n${dh(r.prixNet)} · encaissé ${dh(r.totalEncaisse)}'
                        '${r.remisePct > 0 ? ' · remise ${r.remisePct} % (${libelle(r.statutRemise)})' : ''}'),
                    isThreeLine: true,
                    trailing: StatutChip(r.statut),
                  ),
                )),
          ]);
        },
      ),
    );
  }
}
