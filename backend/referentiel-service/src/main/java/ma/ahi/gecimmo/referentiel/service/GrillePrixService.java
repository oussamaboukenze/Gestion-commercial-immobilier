package ma.ahi.gecimmo.referentiel.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ma.ahi.gecimmo.referentiel.domain.DemandePrix;
import ma.ahi.gecimmo.referentiel.domain.Lot;
import ma.ahi.gecimmo.referentiel.domain.LotStatut;
import ma.ahi.gecimmo.referentiel.exception.BusinessException;
import ma.ahi.gecimmo.referentiel.repository.DemandePrixRepository;
import ma.ahi.gecimmo.referentiel.security.CurrentUser;
import ma.ahi.gecimmo.referentiel.web.dto.ReferentielDtos.DemandePrixRequest;
import ma.ahi.gecimmo.referentiel.web.dto.ReferentielDtos.DemandePrixResponse;

/** Grilles de prix : l'admin SI propose, la direction valide (matrice de validation AHI). */
@Service
@Transactional
public class GrillePrixService {

    private final DemandePrixRepository demandePrixRepository;
    private final LotService lotService;
    private final CurrentUser currentUser;

    public GrillePrixService(DemandePrixRepository demandePrixRepository, LotService lotService,
                             CurrentUser currentUser) {
        this.demandePrixRepository = demandePrixRepository;
        this.lotService = lotService;
        this.currentUser = currentUser;
    }

    public DemandePrixResponse demander(Long lotId, DemandePrixRequest req) {
        Lot lot = lotService.trouverLot(lotId);
        if (lot.getStatut() != LotStatut.DISPONIBLE && lot.getStatut() != LotStatut.OPTION) {
            throw new BusinessException("Le prix d'un lot " + lot.getStatut()
                    + " ne peut plus être modifié (une vente est en cours).");
        }
        if (demandePrixRepository.existsByLotIdAndStatut(lotId, DemandePrix.Statut.EN_ATTENTE)) {
            throw BusinessException.conflict("Une demande de prix est déjà en attente pour ce lot.");
        }
        DemandePrix demande = new DemandePrix(lot, req.nouveauPrix(), req.motif(), currentUser.username());
        return DemandePrixResponse.of(demandePrixRepository.save(demande));
    }

    @Transactional(readOnly = true)
    public List<DemandePrixResponse> enAttente() {
        return demandePrixRepository.findByStatutOrderByDateDemandeAsc(DemandePrix.Statut.EN_ATTENTE).stream()
                .map(DemandePrixResponse::of).toList();
    }

    public DemandePrixResponse decider(Long demandeId, boolean valide) {
        DemandePrix demande = demandePrixRepository.findById(demandeId)
                .orElseThrow(() -> BusinessException.notFound("Demande de prix introuvable : " + demandeId));
        if (demande.getStatut() != DemandePrix.Statut.EN_ATTENTE) {
            throw BusinessException.conflict("Cette demande a déjà été traitée.");
        }
        demande.decider(valide, currentUser.username());
        if (valide) {
            demande.getLot().setPrixTTC(demande.getNouveauPrix());
        }
        return DemandePrixResponse.of(demande);
    }
}
