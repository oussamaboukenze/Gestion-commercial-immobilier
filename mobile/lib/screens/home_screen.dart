import 'package:flutter/material.dart';

import '../api/auth.dart';
import 'accueil_screen.dart';
import 'agenda_screen.dart';
import 'login_screen.dart';
import 'prospects_screen.dart';
import 'stock_screen.dart';
import 'ventes_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _index = 0;

  static const _titres = ['Accueil', 'Stock', 'Prospects', 'Agenda', 'Mes ventes'];

  void _deconnecter() {
    Session.instance.deconnecter();
    Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => const LoginScreen()));
  }

  @override
  Widget build(BuildContext context) {
    final pages = [
      AccueilScreen(onAller: (i) => setState(() => _index = i)),
      const StockScreen(),
      const ProspectsScreen(),
      const AgendaScreen(),
      const VentesScreen(),
    ];
    return Scaffold(
      appBar: AppBar(
        title: Text(_titres[_index]),
        actions: [IconButton(icon: const Icon(Icons.logout), tooltip: 'Déconnexion', onPressed: _deconnecter)],
      ),
      body: IndexedStack(index: _index, children: pages),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (i) => setState(() => _index = i),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.dashboard_outlined), selectedIcon: Icon(Icons.dashboard), label: 'Accueil'),
          NavigationDestination(icon: Icon(Icons.apartment_outlined), selectedIcon: Icon(Icons.apartment), label: 'Stock'),
          NavigationDestination(icon: Icon(Icons.people_outline), selectedIcon: Icon(Icons.people), label: 'Prospects'),
          NavigationDestination(icon: Icon(Icons.event_outlined), selectedIcon: Icon(Icons.event), label: 'Agenda'),
          NavigationDestination(icon: Icon(Icons.assignment_outlined), selectedIcon: Icon(Icons.assignment), label: 'Ventes'),
        ],
      ),
    );
  }
}
