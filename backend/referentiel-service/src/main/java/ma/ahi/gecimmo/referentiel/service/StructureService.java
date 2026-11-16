package ma.ahi.gecimmo.referentiel.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ma.ahi.gecimmo.referentiel.domain.Immeuble;
import ma.ahi.gecimmo.referentiel.domain.Projet;
import ma.ahi.gecimmo.referentiel.domain.Tranche;
import ma.ahi.gecimmo.referentiel.exception.BusinessException;
import ma.ahi.gecimmo.referentiel.repository.ImmeubleRepository;
import ma.ahi.gecimmo.referentiel.repository.ProjetRepository;
import ma.ahi.gecimmo.referentiel.repository.TrancheRepository;
import ma.ahi.gecimmo.referentiel.security.CurrentUser;
import ma.ahi.gecimmo.referentiel.web.dto.ReferentielDtos.ImmeubleRequest;
import ma.ahi.gecimmo.referentiel.web.dto.ReferentielDtos.ImmeubleResponse;
import ma.ahi.gecimmo.referentiel.web.dto.ReferentielDtos.ProjetRequest;
import ma.ahi.gecimmo.referentiel.web.dto.ReferentielDtos.ProjetResponse;
import ma.ahi.gecimmo.referentiel.web.dto.ReferentielDtos.TrancheRequest;
import ma.ahi.gecimmo.referentiel.web.dto.ReferentielDtos.TrancheResponse;

/** Hiérarchie du stock : Projet > Tranche > Immeuble / Îlot (> Étage > Lot, voir LotService). */
@Service
@Transactional
public class StructureService {

    private final ProjetRepository projetRepository;
    private final TrancheRepository trancheRepository;
    private final ImmeubleRepository immeubleRepository;
    private final CurrentUser currentUser;

    public StructureService(ProjetRepository projetRepository, TrancheRepository trancheRepository,
                            ImmeubleRepository immeubleRepository, CurrentUser currentUser) {
        this.projetRepository = projetRepository;
        this.trancheRepository = trancheRepository;
        this.immeubleRepository = immeubleRepository;
        this.currentUser = currentUser;
    }

    // ---------- Projets ----------

    @Transactional(readOnly = true)
    public List<ProjetResponse> listerProjets() {
        List<Projet> projets = currentUser.voitTousLesProjets()
                ? projetRepository.findAllByOrderByCode()
                : projetRepository.findByCodeInOrderByCode(currentUser.projetsAutorises());
        return projets.stream().map(ProjetResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public ProjetResponse getProjet(String code) {
        currentUser.verifierAccesProjet(code.toUpperCase());
        return ProjetResponse.of(trouverProjet(code));
    }

    public ProjetResponse creerProjet(ProjetRequest req) {
        if (projetRepository.existsByCode(req.code().toUpperCase())) {
            throw BusinessException.conflict("Le projet " + req.code() + " existe déjà.");
        }
        Projet projet = new Projet(req.code(), req.nom(), req.ville(), req.type(), req.statut());
        return ProjetResponse.of(projetRepository.save(projet));
    }

    public ProjetResponse modifierProjet(String code, ProjetRequest req) {
        Projet projet = trouverProjet(code);
        projet.setNom(req.nom());
        projet.setVille(req.ville());
        projet.setType(req.type());
        projet.setStatut(req.statut());
        return ProjetResponse.of(projet);
    }

    // ---------- Tranches ----------

    @Transactional(readOnly = true)
    public List<TrancheResponse> listerTranches(String projetCode) {
        currentUser.verifierAccesProjet(projetCode.toUpperCase());
        return trancheRepository.findByProjetCodeOrderByCode(projetCode.toUpperCase()).stream()
                .map(TrancheResponse::of).toList();
    }

    public TrancheResponse creerTranche(String projetCode, TrancheRequest req) {
        Projet projet = trouverProjet(projetCode);
        if (trancheRepository.existsByProjetIdAndCode(projet.getId(), req.code().toUpperCase())) {
            throw BusinessException.conflict("La tranche " + req.code() + " existe déjà dans " + projet.getCode());
        }
        return TrancheResponse.of(trancheRepository.save(new Tranche(req.code(), req.nom(), projet)));
    }

    // ---------- Immeubles ----------

    @Transactional(readOnly = true)
    public List<ImmeubleResponse> listerImmeubles(Long trancheId) {
        Tranche tranche = trouverTranche(trancheId);
        currentUser.verifierAccesProjet(tranche.getProjet().getCode());
        return immeubleRepository.findByTrancheIdOrderByCode(trancheId).stream()
                .map(ImmeubleResponse::of).toList();
    }

    public ImmeubleResponse creerImmeuble(Long trancheId, ImmeubleRequest req) {
        Tranche tranche = trouverTranche(trancheId);
        if (immeubleRepository.existsByTrancheIdAndCode(trancheId, req.code().toUpperCase())) {
            throw BusinessException.conflict("L'immeuble " + req.code() + " existe déjà dans cette tranche.");
        }
        Immeuble immeuble = new Immeuble(req.code(), req.type(), req.nombreEtages(), tranche);
        return ImmeubleResponse.of(immeubleRepository.save(immeuble));
    }

    // ---------- Utilitaires ----------

    Projet trouverProjet(String code) {
        return projetRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> BusinessException.notFound("Projet introuvable : " + code));
    }

    Tranche trouverTranche(Long id) {
        return trancheRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Tranche introuvable : " + id));
    }
}
