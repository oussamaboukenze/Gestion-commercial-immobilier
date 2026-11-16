import 'package:flutter/material.dart';

import '../api/auth.dart';
import 'home_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _identifiant = TextEditingController();
  final _motDePasse = TextEditingController();
  bool _enCours = false;
  bool _masque = true;
  String? _erreur;

  Future<void> _connecter() async {
    setState(() {
      _enCours = true;
      _erreur = null;
    });
    try {
      await Session.instance.connecter(_identifiant.text.trim(), _motDePasse.text);
      if (!Session.instance.estCommercial) {
        Session.instance.deconnecter();
        throw Exception("Application réservée aux commerciaux. Utilisez le site web GECIMMO.");
      }
      if (!mounted) return;
      Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => const HomeScreen()));
    } catch (e) {
      setState(() => _erreur = e.toString().replaceFirst('Exception: ', ''));
    } finally {
      if (mounted) setState(() => _enCours = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final couleur = Theme.of(context).colorScheme.primary;
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
              Icon(Icons.apartment, size: 72, color: couleur),
              const SizedBox(height: 8),
              Text('GECIMMO', textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.bold, color: couleur)),
              const Text('Atlas Horizon Immobilier · Commerciaux', textAlign: TextAlign.center),
              const SizedBox(height: 32),
              TextField(
                controller: _identifiant,
                decoration: const InputDecoration(labelText: 'Identifiant', prefixIcon: Icon(Icons.person), border: OutlineInputBorder()),
                textInputAction: TextInputAction.next,
              ),
              const SizedBox(height: 16),
              TextField(
                controller: _motDePasse,
                obscureText: _masque,
                decoration: InputDecoration(
                  labelText: 'Mot de passe',
                  prefixIcon: const Icon(Icons.lock),
                  border: const OutlineInputBorder(),
                  suffixIcon: IconButton(
                    icon: Icon(_masque ? Icons.visibility : Icons.visibility_off),
                    onPressed: () => setState(() => _masque = !_masque),
                  ),
                ),
                onSubmitted: (_) => _connecter(),
              ),
              if (_erreur != null) ...[
                const SizedBox(height: 12),
                Text(_erreur!, style: const TextStyle(color: Colors.red), textAlign: TextAlign.center),
              ],
              const SizedBox(height: 24),
              FilledButton(
                onPressed: _enCours ? null : _connecter,
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: _enCours
                      ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2))
                      : const Text('Se connecter'),
                ),
              ),
            ]),
          ),
        ),
      ),
    );
  }
}
