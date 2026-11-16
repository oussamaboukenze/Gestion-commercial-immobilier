import 'dart:async';

import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../api/models.dart';
import '../format.dart';
import '../widgets/communs.dart';
import 'prospect_detail.dart';
import 'prospect_form.dart';

/// CRM > Prospects (mes prospects).
class ProspectsScreen extends StatefulWidget {
  const ProspectsScreen({super.key});

  @override
  State<ProspectsScreen> createState() => _ProspectsScreenState();
}

class _ProspectsScreenState extends State<ProspectsScreen> {
  String _recherche = '';
  Timer? _delai;
  late Future<List<Prospect>> _future = _charger();

  Future<List<Prospect>> _charger() async {
    final page = await Api.get('/api/crm/prospects', {'q': _recherche, 'size': '100', 'sort': 'dateCreation,desc'});
    return Api.contenu(page).map(Prospect.fromJson).toList();
  }

  Future<void> _rafraichir() async {
    setState(() => _future = _charger());
    await _future;
  }

  @override
  void dispose() {
    _delai?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      floatingActionButton: FloatingActionButton.extended(
        icon: const Icon(Icons.person_add),
        label: const Text('Nouveau'),
        onPressed: () async {
          final ok = await Navigator.push<bool>(context, MaterialPageRoute(builder: (_) => const ProspectForm()));
          if (ok == true) _rafraichir();
        },
      ),
      body: Column(children: [
        Padding(
          padding: const EdgeInsets.all(12),
          child: TextField(
            decoration: const InputDecoration(
                prefixIcon: Icon(Icons.search), hintText: 'Nom, téléphone ou CIN', border: OutlineInputBorder(), isDense: true),
            onChanged: (v) {
              _delai?.cancel();
              _delai = Timer(const Duration(milliseconds: 400), () {
                _recherche = v.trim();
                _rafraichir();
              });
            },
          ),
        ),
        Expanded(
          child: RefreshIndicator(
            onRefresh: _rafraichir,
            child: Chargement<List<Prospect>>(
              future: _future,
              onReessayer: _rafraichir,
              builder: (liste) => liste.isEmpty
                  ? const Vide('Aucun prospect')
                  : ListView.separated(
                      padding: const EdgeInsets.only(bottom: 80),
                      itemCount: liste.length,
                      separatorBuilder: (_, __) => const Divider(height: 1),
                      itemBuilder: (_, i) {
                        final p = liste[i];
                        return ListTile(
                          leading: CircleAvatar(child: Text(p.nom.isNotEmpty ? p.nom[0].toUpperCase() : '?')),
                          title: Text(p.nomComplet),
                          subtitle: Text('${p.telephone} · ${p.projetCode} · ${p.typologieRecherchee} · ${dh(p.budget)}'),
                          trailing: StatutChip(p.statut),
                          onTap: () async {
                            await Navigator.push(context, MaterialPageRoute(builder: (_) => ProspectDetail(prospect: p)));
                            _rafraichir();
                          },
                        );
                      },
                    ),
            ),
          ),
        ),
      ]),
    );
  }
}
