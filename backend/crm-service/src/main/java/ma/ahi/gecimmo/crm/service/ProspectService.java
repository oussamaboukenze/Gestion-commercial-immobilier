package ma.ahi.gecimmo.crm.service;

import java.util.Collection;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ma.ahi.gecimmo.crm.domain.Prospect;
import ma.ahi.gecimmo.crm.domain.ProspectStatut;
import ma.ahi.gecimmo.crm.exception.BusinessException;
import ma.ahi.gecimmo.crm.repository.ProspectRepository;
import ma.ahi.gecimmo.crm.repository.RelanceRepository;
import ma.ahi.gecimmo.crm.security.CurrentUser;
import ma.ahi.gecimmo.crm.web.dto.CrmDtos.ProspectRequest;
import ma.ahi.gecimmo.crm.web.dto.CrmDtos.ProspectResponse;
import ma.ahi.gecimmo.crm.web.dto.CrmDtos.ReaffectationResponse;

@Service
@Transactional
public class ProspectService {

    private final ProspectRepository prospectRepository;
    private final RelanceRepository relanceRepository;
    private final CurrentUser currentUser;

    public ProspectService(ProspectRepository prospectRepository, RelanceRepository relanceRepository,
                           CurrentUser currentUser) {
        this.prospectRepository = prospectRepository;
        this.relanceRepository = relanceRepository;
        this.currentUser = currentUser;
    }

    /** Un commercial "simple" ne voit que ses propres prospects. */
    private boolean estCommercialSimple() {
        return currentUser.hasRole("COMMERCIAL")
                && !currentUser.hasRole("RESP_COMMERCIAL")
                && !currentUser.hasRole("DIRECTION");
    }

    @Transactional(readOnly = true)
    public Page<ProspectResponse> rechercher(String q, ProspectStatut statut, String projet, Pageable pageable) {
        boolean tous = false;
        Collection<String> projets;
        if (projet != null && !projet.isBlank()) {
            currentUser.verifierAccesProjet(projet.toUpperCase());
            projets = Set.of(projet.toUpperCase());
        } else if (currentUser.voitTousLesProjets()) {
            tous = true;
            projets = Set.of("*");
        } else {
            projets = currentUser.projetsAutorises();
            if (projets.isEmpty()) {
                return Page.empty(pageable);
            }
        }
        String commercial = estCommercialSimple() ? currentUser.username() : null;
        String recherche = (q == null || q.isBlank()) ? null : q.trim();
        return prospectRepository.rechercher(tous, projets, commercial, statut, recherche, pageable)
                .map(ProspectResponse::of);
    }

    @Transactional(readOnly = true)
    public ProspectResponse get(Long id) {
        return ProspectResponse.of(trouverAvecControle(id));
    }

    public ProspectResponse creer(ProspectRequest req) {
        String projet = req.projetCode().toUpperCase();
        currentUser.verifierAccesProjet(projet);
        if (prospectRepository.existsByCinAndProjetCode(req.cin().toUpperCase(), projet)) {
            throw BusinessException.conflict("Un prospect avec la CIN " + req.cin()
                    + " existe déjà sur le projet " + projet + ".");
        }
        Prospect p = new Prospect(req.nom(), req.prenom(), req.telephone(), req.cin(), req.email(),
                req.source(), req.budget(), req.typologieRecherchee(), projet, currentUser.username());
        return ProspectResponse.of(prospectRepository.save(p));
    }

    public ProspectResponse modifier(Long id, ProspectRequest req) {
        Prospect p = trouverAvecControle(id);
        p.setNom(req.nom());
        p.setPrenom(req.prenom());
        p.setTelephone(req.telephone());
        p.setCin(req.cin());
        p.setEmail(req.email());
        p.setSource(req.source());
        p.setBudget(req.budget());
        p.setTypologieRecherchee(req.typologieRecherchee());
        if (p.getStatut() == ProspectStatut.NOUVEAU) {
            p.setStatut(ProspectStatut.EN_COURS);
        }
        return ProspectResponse.of(p);
    }

    public ProspectResponse marquerPerdu(Long id, String motif) {
        Prospect p = trouverAvecControle(id);
        if (p.getStatut() == ProspectStatut.CONVERTI) {
            throw new BusinessException("Ce prospect a déjà une réservation : passez par un désistement (ADV).");
        }
        p.setStatut(ProspectStatut.PERDU);
        p.setMotifPerte(motif);
        return ProspectResponse.of(p);
    }

    /** Admin SI : départ d'un collaborateur -> ses prospects et relances vont à un autre commercial. */
    public ReaffectationResponse reaffecter(String ancien, String nouveau) {
        int prospects = prospectRepository.reaffecter(ancien, nouveau);
        int relances = relanceRepository.reaffecter(ancien, nouveau);
        return new ReaffectationResponse(prospects, relances);
    }

    /** Appelé quand l'ADV valide la réservation (événement RabbitMQ). */
    public void convertir(Long prospectId) {
        prospectRepository.findById(prospectId).ifPresent(Prospect::convertir);
    }

    /** Lecture pour un autre microservice (vente-service). */
    @Transactional(readOnly = true)
    public ProspectResponse getInterne(Long id) {
        return ProspectResponse.of(prospectRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Prospect introuvable : " + id)));
    }

    /** Contrôle d'accès : projet autorisé + propriétaire si commercial simple. */
    public Prospect trouverAvecControle(Long id) {
        Prospect p = prospectRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Prospect introuvable : " + id));
        currentUser.verifierAccesProjet(p.getProjetCode());
        if (estCommercialSimple() && !p.getCommercial().equals(currentUser.username())) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Ce prospect appartient à un autre commercial.");
        }
        return p;
    }
}
