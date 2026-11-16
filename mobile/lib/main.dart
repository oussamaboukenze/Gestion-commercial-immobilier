import 'package:flutter/material.dart';

import 'api/auth.dart';
import 'screens/home_screen.dart';
import 'screens/login_screen.dart';

void main() => runApp(const GecimmoApp());

class GecimmoApp extends StatelessWidget {
  const GecimmoApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'GECIMMO Commercial',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF1F4E79)),
        useMaterial3: true,
      ),
      home: Session.instance.connecte ? const HomeScreen() : const LoginScreen(),
    );
  }
}
