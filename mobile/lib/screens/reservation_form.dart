import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../api/models.dart';
import '../format.dart';
import '../widgets/communs.dart';
import 'stock_screen.dart';

/// Ventes > Réservations > Nouvelle : lot + client + avance -> "En attente de validation ADV".
class ReservationForm extends StatefulWidget {
  final Lot lot;
  final Prospect? prospect;
  const ReservationForm({super.key, required this.lot, this.prospect});

  @override
  State<ReservationForm> createState() => _ReservationFormState();
}

class _ReservationFormState extends State<ReservationForm> {
  Prospect? _prospect;
  final _remise = TextEditingController(text: '0');
  late final TextEditingController _avance;
  bool _cin = false;
  bool _revenus = false;
  bool _recu = false;
  bool _enCours = false;

  @override
  void initState() {
    super.initState();
    _prospect = widget.prospect;
    _avance = TextEditingController(text: (widget.lot.prixTTC * 0.05).ceil().toString());
  }

  num get _remisePct => num.tryParse(_remise.text.replaceAll(',', '.')) ?? 0;
  num get _prixNet => widget.lot.prixTTC * (1 - _remisePct / 100);
  num get _avanceMin => _prixNet * 0.05;

  String get _niveauRemise {
    if (_remisePct <= 2) return 'Remise dans votre pouvoir (≤ 2 %)';
    if (_remisePct <= 5) return 'Validation du responsable commercial (2–5 %)';
    return 'Validation de la direction (> 5 %)';
  }

  Future<void> _enregistrer() async {
    if (_prospect == null) {
      afficherMessage(context, 'Choisissez le client', erreur: true);
      return;
    }
    setState(() => _enCours = true);
    final ok = await executer(
      context,
      () => Api.post('/api/ventes/reservations', {
        'lotId': widget.lot.id,
        'prospectId': _prospect!.id,
        'remisePct': _remisePct,
        'avance': num.tryParse(_avance.text.replaceAll(',', '.')) ?? 0,
        'pieceCin': _cin,
        'pieceJustificatifRevenus': _revenus,
        'pieceRecuAvance': _recu,
      }),
      'Réservation créée : en attente de validation ADV',
    );
    if (!mounted) return;
    setState(() => _enCours = false);
    if (ok) Navigator.pop(context, true);
  }

  @override
  Widget build(BuildContext context) {
    final lot = widget.lot;
    return Scaffold(
      appBar: AppBar(title: const Text('Nouvelle réservation')),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        Card(
          child: ListTile(
            leading: const Icon(Icons.home_work),
            title: Text(lot.code),
            subtitle: Text('${lot.typologie} · ${dh(lot.prixTTC)}'),
            trailing: StatutChip(lot.statut),
          ),
        ),
        const SizedBox(height: 8),
        Card(
          child: ListTile(
            leading: const Icon(Icons.person),
            title: Text(_prospect?.nomComplet ?? 'Choisir le client'),
            subtitle: Text(_prospect == null ? 'Prospect du projet ${lot.projetCode}' : 'CIN ${_prospect!.cin}'),
            trailing: const Icon(Icons.chevron_right),
            onTap: () async {
              final p = await choisirProspect(context, lot.projetCode);
              if (p != null) setState(() => _prospect = p);
            },
          ),
        ),
        const SizedBox(height: 16),
        TextField(
          controller: _remise,
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
          decoration: InputDecoration(labelText: 'Remise (%)', helperText: _niveauRemise, border: const OutlineInputBorder()),
          onChanged: (_) => setState(() {}),
        ),
        const SizedBox(height: 16),
        TextField(
          controller: _avance,
          keyboardType: TextInputType.number,
          decoration: InputDecoration(
            labelText: 'Avance (DH)',
            helperText: 'Minimum 5 % du prix net : ${dh(_avanceMin)}',
            border: const OutlineInputBorder(),
          ),
        ),
        const SizedBox(height: 8),
        Text('Prix net : ${dh(_prixNet)}', style: const TextStyle(fontWeight: FontWeight.bold)),
        const SizedBox(height: 16),
        const Text('Pièces fournies'),
        CheckboxListTile(value: _cin, title: const Text('CIN'), onChanged: (v) => setState(() => _cin = v ?? false)),
        CheckboxListTile(value: _revenus, title: const Text('Justificatif de revenus'), onChanged: (v) => setState(() => _revenus = v ?? false)),
        CheckboxListTile(value: _recu, title: const Text("Reçu de l'avance"), onChanged: (v) => setState(() => _recu = v ?? false)),
        const SizedBox(height: 16),
        FilledButton.icon(
          icon: const Icon(Icons.send),
          label: Text(_enCours ? 'Envoi…' : 'Envoyer à l\'ADV'),
          onPressed: _enCours ? null : _enregistrer,
        ),
      ]),
    );
  }
}
