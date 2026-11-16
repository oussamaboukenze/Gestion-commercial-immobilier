/// Modèles (champs utiles à l'application mobile).
num _num(dynamic v) => v is num ? v : num.tryParse('$v') ?? 0;

class Lot {
  final int id;
  final String code;
  final String projetCode;
  final String typologie;
  final int etage;
  final num? surface;
  final num prixTTC;
  final String statut;

  Lot.fromJson(Map<String, dynamic> j)
      : id = j['id'] as int,
        code = j['code'] as String,
        projetCode = j['projetCode'] as String,
        typologie = j['typologie'] as String,
        etage = (j['etage'] ?? 0) as int,
        surface = j['surface'] == null ? null : _num(j['surface']),
        prixTTC = _num(j['prixTTC']),
        statut = j['statut'] as String;
}

class Prospect {
  final int id;
  final String nom;
  final String? prenom;
  final String telephone;
  final String cin;
  final String? email;
  final String source;
  final num budget;
  final String typologieRecherchee;
  final String projetCode;
  final String commercial;
  final String statut;

  Prospect.fromJson(Map<String, dynamic> j)
      : id = j['id'] as int,
        nom = j['nom'] as String,
        prenom = j['prenom'] as String?,
        telephone = j['telephone'] as String,
        cin = j['cin'] as String,
        email = j['email'] as String?,
        source = j['source'] as String,
        budget = _num(j['budget']),
        typologieRecherchee = (j['typologieRecherchee'] ?? '') as String,
        projetCode = j['projetCode'] as String,
        commercial = j['commercial'] as String,
        statut = j['statut'] as String;

  String get nomComplet => (prenom == null || prenom!.isEmpty) ? nom : '$prenom $nom';
}

class Reservation {
  final int id;
  final String numero;
  final String lotCode;
  final String clientNom;
  final num prixNet;
  final num remisePct;
  final String statutRemise;
  final num avance;
  final num totalEncaisse;
  final String statut;
  final String dateCreation;

  Reservation.fromJson(Map<String, dynamic> j)
      : id = j['id'] as int,
        numero = (j['numero'] ?? '') as String,
        lotCode = j['lotCode'] as String,
        clientNom = j['clientNom'] as String,
        prixNet = _num(j['prixNet']),
        remisePct = _num(j['remisePct']),
        statutRemise = (j['statutRemise'] ?? '') as String,
        avance = _num(j['avance']),
        totalEncaisse = _num(j['totalEncaisse']),
        statut = j['statut'] as String,
        dateCreation = (j['dateCreation'] ?? '') as String;
}

class OptionLot {
  final int id;
  final int lotId;
  final String lotCode;
  final int prospectId;
  final String dateExpiration;

  OptionLot.fromJson(Map<String, dynamic> j)
      : id = j['id'] as int,
        lotId = j['lotId'] as int,
        lotCode = j['lotCode'] as String,
        prospectId = j['prospectId'] as int,
        dateExpiration = j['dateExpiration'] as String;
}

class RendezVous {
  final int id;
  final int prospectId;
  final String prospectNom;
  final String dateHeure;
  final String type;
  final String statut;
  final String? compteRendu;

  RendezVous.fromJson(Map<String, dynamic> j)
      : id = j['id'] as int,
        prospectId = j['prospectId'] as int,
        prospectNom = j['prospectNom'] as String,
        dateHeure = j['dateHeure'] as String,
        type = j['type'] as String,
        statut = j['statut'] as String,
        compteRendu = j['compteRendu'] as String?;
}

class RelanceCrm {
  final int id;
  final String prospectNom;
  final String telephone;
  final String dateRelance;
  final String objet;
  final bool faite;

  RelanceCrm.fromJson(Map<String, dynamic> j)
      : id = j['id'] as int,
        prospectNom = j['prospectNom'] as String,
        telephone = (j['telephone'] ?? '') as String,
        dateRelance = j['dateRelance'] as String,
        objet = j['objet'] as String,
        faite = (j['faite'] ?? false) as bool;
}

const typologies = ['STUDIO', 'F2', 'F3', 'F4', 'DUPLEX', 'VILLA', 'COMMERCE', 'BUREAU', 'PARKING'];
const sources = ['SITE_WEB', 'RESEAUX_SOCIAUX', 'BOUCHE_A_OREILLE', 'SALON', 'PANNEAU_CHANTIER', 'APPEL_ENTRANT', 'PARRAINAGE', 'AUTRE'];
