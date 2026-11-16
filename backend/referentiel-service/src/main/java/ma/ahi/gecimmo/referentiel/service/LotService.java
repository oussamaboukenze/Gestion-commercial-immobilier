package ma.ahi.gecimmo.referentiel.service;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ma.ahi.gecimmo.referentiel.domain.Immeuble;
import ma.ahi.gecimmo.referentiel.domain.Lot;
import ma.ahi.gecimmo.referentiel.domain.LotStatut;
import ma.ahi.gecimmo.referentiel.domain.Typologie;
import ma.ahi.gecimmo.referentiel.exception.BusinessException;
import ma.ahi.gecimmo.referentiel.repository.ImmeubleRepository;
import ma.ahi.gecimmo.referentiel.repository.LotRepository;
import ma.ahi.gecimmo.referentiel.security.CurrentUser;
import ma.ahi.gecimmo.referentiel.web.dto.ReferentielDtos.LotRequest;
import ma.ahi.gecimmo.referentiel.web.dto.ReferentielDtos.LotResponse;
import ma.ahi.gecimmo.referentiel.web.dto.ReferentielDtos.LotUpdateRequest;
import ma.ahi.gecimmo.referentiel.web.dto.ReferentielDtos.StockResponse;

@Service
@Transactional
public class LotService {

    private static final Logger log = LoggerFactory.getLogger(LotService.class);

    private final LotRepository lotRepository;
    private final ImmeubleRepository immeubleRepository;
    private final CurrentUser currentUser;

    public LotService(LotRepository lotRepository, ImmeubleRepository immeubleRepository,
                      CurrentUser currentUser) {
        this.lotRepository = lotRepository;
        this.immeubleRepository = immeubleRepository;
        this.currentUser = currentUser;
    }

    /** Référentiel > Lots > filtrer par projet, typologie, statut. */
    @Transactional(readOnly = true)
    public Page<LotResponse> rechercher(String projetCode, Typologie typologie, LotStatut statut,
                                        Pageable pageable) {
        if (projetCode != null && !projetCode.isBlank()) {
            currentUser.verifierAccesProjet(projetCode.toUpperCase());
            return lotRepository.rechercher(Set.of(projetCode.toUpperCase()), typologie, statut, pageable)
                    .map(LotResponse::of);
        }
        if (currentUser.voitTousLesProjets()) {
            return lotRepository.rechercherTous(typologie, statut, pageable).map(LotResponse::of);
        }
        Collection<String> projets = currentUser.projetsAutorises();
        if (projets.isEmpty()) {
            return Page.empty(pageable);
        }
        return lotRepository.rechercher(projets, typologie, statut, pageable).map(LotResponse::of);
    }

    @Transactional(readOnly = true)
    public LotResponse getLot(Long id) {
        Lot lot = trouverLot(id);
        currentUser.verifierAccesProjet(lot.getProjetCode());
        return LotResponse.of(lot);
    }

    /** Lecture sans contrôle de projet (appel interne entre services). */
    @Transactional(readOnly = true)
    public LotResponse lotInterne(Long id) {
        return LotResponse.of(trouverLot(id));
    }

    @Transactional(readOnly = true)
    public List<LotResponse> listerLotsImmeuble(Long immeubleId) {
        Immeuble immeuble = trouverImmeuble(immeubleId);
        currentUser.verifierAccesProjet(immeuble.getTranche().getProjet().getCode());
        return lotRepository.findByImmeubleIdOrderByEtageAscNumeroAsc(immeubleId).stream()
                .map(LotResponse::of).toList();
    }

    public LotResponse creerLot(Long immeubleId, LotRequest req) {
        Immeuble immeuble = trouverImmeuble(immeubleId);
        Lot lot = new Lot(immeuble, req.numero(), req.etage(), req.typologie(), req.surface(), req.prixTTC());
        if (lotRepository.existsByCode(lot.getCode())) {
            throw BusinessException.conflict("Le lot " + lot.getCode() + " existe déjà.");
        }
        return LotResponse.of(lotRepository.save(lot));
    }

    public LotResponse modifierLot(Long id, LotUpdateRequest req) {
        Lot lot = trouverLot(id);
        lot.setTypologie(req.typologie());
        lot.setSurface(req.surface());
        return LotResponse.of(lot);
    }

    /** Reporting > Stock : nombre de lots par statut. */
    @Transactional(readOnly = true)
    public StockResponse etatStock(String projetCode) {
        String code = projetCode.toUpperCase();
        currentUser.verifierAccesProjet(code);
        Map<LotStatut, Long> parStatut = new EnumMap<>(LotStatut.class);
        for (LotStatut s : LotStatut.values()) {
            parStatut.put(s, 0L);
        }
        long total = 0;
        for (Object[] row : lotRepository.compterParStatut(code)) {
            long nb = (Long) row[1];
            parStatut.put((LotStatut) row[0], nb);
            total += nb;
        }
        return new StockResponse(code, total, parStatut);
    }

    /**
     * Changement de statut demandé par un autre microservice (vente-service).
     * "Compare-and-set" : le changement n'est fait que si le statut actuel fait partie
     * des statuts attendus, sinon 409. Avec @Version, deux commerciaux ne peuvent pas
     * réserver le même lot au même moment.
     */
    public LotResponse changerStatut(Long lotId, LotStatut cible, Collection<LotStatut> statutsAttendus) {
        Lot lot = trouverLot(lotId);
        if (statutsAttendus != null && !statutsAttendus.isEmpty() && !statutsAttendus.contains(lot.getStatut())) {
            throw BusinessException.conflict("Le lot " + lot.getCode() + " n'est plus disponible (statut actuel : "
                    + lot.getStatut() + ").");
        }
        lot.changerStatut(cible);
        lotRepository.saveAndFlush(lot);
        log.info("Lot {} : statut -> {}", lot.getCode(), cible);
        return LotResponse.of(lot);
    }

    Lot trouverLot(Long id) {
        return lotRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Lot introuvable : " + id));
    }

    private Immeuble trouverImmeuble(Long id) {
        return immeubleRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Immeuble introuvable : " + id));
    }
}
