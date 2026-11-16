package ma.ahi.gecimmo.vente.client;

/** Prospect tel que renvoyé par crm-service. */
public record ProspectDto(Long id, String nom, String prenom, String telephone, String cin, String projetCode,
                          String commercial, String statut) {

    public String nomComplet() {
        return prenom == null || prenom.isBlank() ? nom : prenom + " " + nom;
    }
}
