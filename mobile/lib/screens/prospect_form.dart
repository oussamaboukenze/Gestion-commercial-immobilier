import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../api/auth.dart';
import '../api/models.dart';
import '../format.dart';
import '../widgets/communs.dart';

/// CRM > Prospects > Nouveau / Modifier.
class ProspectForm extends StatefulWidget {
  final Prospect? prospect;
  const ProspectForm({super.key, this.prospect});

  @override
  State<ProspectForm> createState() => _ProspectFormState();
}

class _ProspectFormState extends State<ProspectForm> {
  final _form = GlobalKey<FormState>();
  late final TextEditingController _nom = TextEditingController(text: widget.prospect?.nom);
  late final TextEditingController _prenom = TextEditingController(text: widget.prospect?.prenom);
  late final TextEditingController _telephone = TextEditingController(text: widget.prospect?.telephone);
  late final TextEditingController _cin = TextEditingController(text: widget.prospect?.cin);
  late final TextEditingController _email = TextEditingController(text: widget.prospect?.email);
  late final TextEditingController _budget = TextEditingController(text: widget.prospect?.budget.toString());
  late String _source = widget.prospect?.source ?? 'SITE_WEB';
  late String _typologie = widget.prospect?.typologieRecherchee.isNotEmpty == true ? widget.prospect!.typologieRecherchee : 'F3';
  late String? _projet = widget.prospect?.projetCode ??
      (Session.instance.projets.isNotEmpty ? Session.instance.projets.first : null);
  bool _enCours = false;

  String? _requis(String? v) => (v == null || v.trim().isEmpty) ? 'Obligatoire' : null;

  Future<void> _enregistrer() async {
    if (!_form.currentState!.validate() || _projet == null) return;
    setState(() => _enCours = true);
    final corps = {
      'nom': _nom.text.trim(),
      'prenom': _prenom.text.trim(),
      'telephone': _telephone.text.trim(),
      'cin': _cin.text.trim().toUpperCase(),
      'email': _email.text.trim().isEmpty ? null : _email.text.trim(),
      'source': _source,
      'budget': num.tryParse(_budget.text.replaceAll(' ', '')) ?? 0,
      'typologieRecherchee': _typologie,
      'projetCode': _projet,
    };
    final p = widget.prospect;
    final ok = await executer(context,
        () => p == null ? Api.post('/api/crm/prospects', corps) : Api.put('/api/crm/prospects/${p.id}', corps),
        'Prospect enregistré');
    if (!mounted) return;
    setState(() => _enCours = false);
    if (ok) Navigator.pop(context, true);
  }

  Widget _champ(TextEditingController c, String label,
          {String? Function(String?)? validator, TextInputType? clavier}) =>
      Padding(
        padding: const EdgeInsets.only(bottom: 12),
        child: TextFormField(
          controller: c,
          validator: validator,
          keyboardType: clavier,
          decoration: InputDecoration(labelText: label, border: const OutlineInputBorder()),
        ),
      );

  Widget _liste(String label, String? valeur, List<String> options, void Function(String?) onChange,
          {String Function(String)? texte}) =>
      Padding(
        padding: const EdgeInsets.only(bottom: 12),
        child: DropdownButtonFormField<String>(
          value: valeur,
          isExpanded: true,
          decoration: InputDecoration(labelText: label, border: const OutlineInputBorder()),
          items: options.map((o) => DropdownMenuItem(value: o, child: Text(texte == null ? o : texte(o)))).toList(),
          onChanged: (v) => setState(() => onChange(v)),
          validator: (v) => v == null ? 'Obligatoire' : null,
        ),
      );

  @override
  Widget build(BuildContext context) {
    final edition = widget.prospect != null;
    return Scaffold(
      appBar: AppBar(title: Text(edition ? 'Modifier le prospect' : 'Nouveau prospect')),
      body: Form(
        key: _form,
        child: ListView(padding: const EdgeInsets.all(16), children: [
          _champ(_nom, 'Nom *', validator: _requis),
          _champ(_prenom, 'Prénom'),
          _champ(_telephone, 'Téléphone *', validator: _requis, clavier: TextInputType.phone),
          _champ(_cin, 'CIN * (ex. BE123456)', validator: (v) {
            if (_requis(v) != null) return 'Obligatoire';
            return RegExp(r'^[A-Za-z]{1,2}[0-9]{3,8}$').hasMatch(v!.trim()) ? null : 'CIN invalide';
          }),
          _champ(_email, 'Email', clavier: TextInputType.emailAddress),
          _champ(_budget, 'Budget (DH) *', validator: _requis, clavier: TextInputType.number),
          _liste('Source *', _source, sources, (v) => _source = v ?? _source, texte: libelle),
          _liste('Typologie recherchée *', _typologie, typologies, (v) => _typologie = v ?? _typologie),
          if (!edition) _liste('Projet *', _projet, Session.instance.projets, (v) => _projet = v),
          const SizedBox(height: 8),
          FilledButton(
            onPressed: _enCours ? null : _enregistrer,
            child: Padding(padding: const EdgeInsets.all(12), child: Text(_enCours ? 'Enregistrement…' : 'Enregistrer')),
          ),
        ]),
      ),
    );
  }
}
