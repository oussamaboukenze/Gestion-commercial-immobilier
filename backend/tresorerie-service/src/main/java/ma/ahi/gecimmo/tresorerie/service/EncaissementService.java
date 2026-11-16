package ma.ahi.gecimmo.tresorerie.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ma.ahi.gecimmo.tresorerie.client.ReservationDto;
import ma.ahi.gecimmo.tresorerie.client.VenteClient;
import ma.ahi.gecimmo.tresorerie.config.TresorerieProperties;
import ma.ahi.gecimmo.tresorerie.domain.DemandeCorrection;
import ma.ahi.gecimmo.tresorerie.domain.Encaissement;
import ma.ahi.gecimmo.tresorerie.domain.Impaye;
import ma.ahi.gecimmo.tresorerie.domain.ModePaiement;
import ma.ahi.gecimmo.tresorerie.domain.RemiseEnBanque;
import ma.ahi.gecimmo.tresorerie.exception.BusinessException;
import ma.ahi.gecimmo.tresorerie.messaging.TresorerieEventPublisher;
import ma.ahi.gecimmo.tresorerie.repository.DemandeCorrectionRepository;
import ma.ahi.gecimmo.tresorerie.repository.EncaissementRepository;
import ma.ahi.gecimmo.tresorerie.repository.ImpayeRepository;
import ma.ahi.gecimmo.tresorerie.repository.RemiseEnBanqueRepository;
import ma.ahi.gecimmo.tresorerie.security.CurrentUser;
import ma.ahi.gecimmo.tresorerie.web.dto.TresorerieDtos.CorrectionResponse;
import ma.ahi.gecimmo.tresorerie.web.dto.TresorerieDtos.EncaissementRequest;
import ma.ahi.gecimmo.tresorerie.web.dto.TresorerieDtos.EncaissementResponse;
import ma.ahi.gecimmo.tresorerie.web.dto.TresorerieDtos.ImpayeRequest;
import ma.ahi.gecimmo.tresorerie.web.dto.TresorerieDtos.ImpayeResponse;
import ma.ahi.gecimmo.tresorerie.web.dto.TresorerieDtos.RemiseRequest;
import ma.ahi.gecimmo.tresorerie.web.dto.TresorerieDtos.RemiseResponse;

/** Trésorerie > Encaissements / Remises en banque / Impayés / corrections. */
@Service
@Transactional
public class EncaissementService {

    private final EncaissementRepository encaissementRepository;
    private final RemiseEnBanqueRepository remiseRepository;
    private final ImpayeRepository impayeRepository;
    private final DemandeCorrectionRepository correctionRepository;
    private final RelanceService relanceService;
    private final VenteClient venteClient;
    private final TresorerieEventPublisher eventPublisher;
    private final TresorerieProperties properties;
    private final CurrentUser currentUser;

    public EncaissementService(EncaissementRepository encaissementRepository,
                               RemiseEnBanqueRepository remiseRepository, ImpayeRepository impayeRepository,
                               DemandeCorrectionRepository correctionRepository, RelanceService relanceService,
                               VenteClient venteClient, TresorerieEventPublisher eventPublisher,
                               TresorerieProperties properties, CurrentUser currentUser) {
        this.encaissementRepository = encaissementRepository;
        this.remiseRepository = remiseRepository;
        this.impayeRepository = impayeRepository;
        this.correctionRepository = correctionRepository;
        this.relanceService = relanceService;
        this.venteClient = venteClient;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.currentUser = currentUser;
    }

    // ---------- Encaissements ----------

    /** Trésorerie > Encaissements > Nouveau -> reçu généré. */
    public EncaissementResponse enregistrer(EncaissementRequest req) {
        ReservationDto dossier = venteClient.getReservation(req.reservationId());
        currentUser.verifierAccesProjet(dossier.projetCode());
        if (!dossier.estActive()) {
            throw new BusinessException("Le dossier " + dossier.numero() + " n'est pas actif (" + dossier.statut() + ").");
        }
        if (req.montant().compareTo(dossier.resteAPayer()) > 0) {
            throw new BusinessException("Montant supérieur au reste à payer du dossier (" + dossier.resteAPayer()
                    + " DH).");
        }
        controlerMode(req.mode(), req.montant(), req.numeroCheque(), req.banque());
        Encaissement e = new Encaissement(dossier.id(), dossier.numero(), dossier.clientNom(), dossier.projetCode(),
                req.echeanceId(), req.montant(), req.mode(), req.numeroCheque(), req.banque(), req.reference(),
                req.dateEncaissement(), Encaissement.Origine.CLIENT, currentUser.username());
        return EncaissementResponse.of(sauvegarder(e));
    }

    /** Utilisé par le déblocage d'un crédit bancaire. */
    Encaissement enregistrerDeblocage(ReservationDto dossier, BigDecimal montant, String banque, String reference,
                                      LocalDate date) {
        Encaissement e = new Encaissement(dossier.id(), dossier.numero(), dossier.clientNom(), dossier.projetCode(),
                null, montant, ModePaiement.VIREMENT, null, banque, reference, date,
                Encaissement.Origine.CREDIT_BANCAIRE, currentUser.username());
        return sauvegarder(e);
    }

    private Encaissement sauvegarder(Encaissement e) {
        e = encaissementRepository.saveAndFlush(e);
        e.attribuerNumero();
        eventPublisher.encaissementEnregistre(e);
        return e;
    }

    private void controlerMode(ModePaiement mode, BigDecimal montant, String numeroCheque, String banque) {
        if (mode == ModePaiement.CHEQUE && (isBlank(numeroCheque) || isBlank(banque))) {
            throw new BusinessException("Chèque : numéro et banque obligatoires.");
        }
        if (mode == ModePaiement.ESPECES && properties.plafondEspecesActif()
                && montant.compareTo(properties.plafondEspeces()) > 0) {
            throw new BusinessException("Paiement en espèces au-delà de " + properties.plafondEspeces()
                    + " DH interdit (politique interne AHI).");
        }
    }

    @Transactional(readOnly = true)
    public Page<EncaissementResponse> rechercher(String projet, LocalDate debut, LocalDate fin, ModePaiement mode,
                                                 Pageable pageable) {
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
        return encaissementRepository.rechercher(tous, projets, debut, fin, mode, pageable)
                .map(EncaissementResponse::of);
    }

    @Transactional(readOnly = true)
    public List<EncaissementResponse> parDossier(Long reservationId) {
        List<Encaissement> liste = encaissementRepository.findByReservationIdOrderByDateEncaissement(reservationId);
        liste.forEach(e -> currentUser.verifierAccesProjet(e.getProjetCode()));
        return liste.stream().map(EncaissementResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public EncaissementResponse get(Long id) {
        return EncaissementResponse.of(trouver(id));
    }

    // ---------- Remises en banque ----------

    @Transactional(readOnly = true)
    public List<EncaissementResponse> chequesARemettre() {
        return encaissementRepository.findByModeAndStatutAndRemiseIsNullOrderByDateEncaissement(ModePaiement.CHEQUE,
                        Encaissement.Statut.VALIDE).stream()
                .filter(e -> currentUser.peutVoirProjet(e.getProjetCode()))
                .map(EncaissementResponse::of).toList();
    }

    /** Trésorerie > Remises en banque > sélectionner les chèques. */
    public RemiseResponse remettreEnBanque(RemiseRequest req) {
        RemiseEnBanque remise = new RemiseEnBanque(req.banque(), req.dateRemise(), currentUser.username());
        for (Long id : req.encaissementIds()) {
            Encaissement e = trouver(id);
            if (e.getMode() != ModePaiement.CHEQUE || e.getStatut() != Encaissement.Statut.VALIDE) {
                throw new BusinessException("Le reçu " + e.getNumeroRecu() + " n'est pas un chèque valide.");
            }
            if (e.getRemise() != null) {
                throw BusinessException.conflict("Le chèque du reçu " + e.getNumeroRecu() + " est déjà remis en banque.");
            }
            remise.ajouter(e);
        }
        remise = remiseRepository.saveAndFlush(remise);
        remise.attribuerNumero();
        return RemiseResponse.of(remise);
    }

    @Transactional(readOnly = true)
    public List<RemiseResponse> remises() {
        return remiseRepository.findAllByOrderByDateRemiseDesc().stream().map(RemiseResponse::of).toList();
    }

    // ---------- Impayés ----------

    /** Chèque rejeté : l'échéance redevient due + relance automatique. */
    public ImpayeResponse declarerImpaye(ImpayeRequest req) {
        Encaissement e = trouver(req.encaissementId());
        if (e.getMode() != ModePaiement.CHEQUE) {
            throw new BusinessException("Seul un chèque peut être déclaré impayé.");
        }
        if (e.getStatut() != Encaissement.Statut.VALIDE) {
            throw BusinessException.conflict("Cet encaissement est déjà " + e.getStatut() + ".");
        }
        e.declarerImpaye(req.dateRejet());
        Impaye impaye = impayeRepository.save(new Impaye(e, req.motif(), req.dateRejet(), currentUser.username()));
        eventPublisher.encaissementAnnule(e);
        relanceService.relanceChequeImpaye(e, req.motif());
        return ImpayeResponse.of(impaye);
    }

    @Transactional(readOnly = true)
    public List<ImpayeResponse> impayes() {
        return impayeRepository.findAllByOrderByDateRejetDesc().stream()
                .filter(i -> currentUser.peutVoirProjet(i.getEncaissement().getProjetCode()))
                .map(ImpayeResponse::of).toList();
    }

    // ---------- Corrections (contre-passation, validation DAF) ----------

    public CorrectionResponse demanderCorrection(Long encaissementId, String motif) {
        Encaissement e = trouver(encaissementId);
        if (e.getStatut() != Encaissement.Statut.VALIDE) {
            throw BusinessException.conflict("Seul un encaissement validé peut être contre-passé.");
        }
        if (correctionRepository.existsByEncaissementIdAndStatut(encaissementId,
                DemandeCorrection.Statut.EN_ATTENTE_DAF)) {
            throw BusinessException.conflict("Une demande de correction est déjà en attente du DAF.");
        }
        DemandeCorrection d = new DemandeCorrection(e, motif, currentUser.username());
        return CorrectionResponse.of(correctionRepository.save(d));
    }

    public CorrectionResponse deciderCorrection(Long demandeId, boolean valide) {
        DemandeCorrection d = correctionRepository.findById(demandeId)
                .orElseThrow(() -> BusinessException.notFound("Demande de correction introuvable : " + demandeId));
        currentUser.verifierAccesProjet(d.getEncaissement().getProjetCode());
        if (d.getStatut() != DemandeCorrection.Statut.EN_ATTENTE_DAF) {
            throw BusinessException.conflict("Cette demande a déjà été traitée.");
        }
        d.decider(valide, currentUser.username());
        if (valide) {
            Encaissement e = d.getEncaissement();
            if (e.getStatut() != Encaissement.Statut.VALIDE) {
                throw BusinessException.conflict("L'encaissement n'est plus valide (" + e.getStatut() + ").");
            }
            e.contrePasser(LocalDate.now());
            eventPublisher.encaissementAnnule(e);
        }
        return CorrectionResponse.of(d);
    }

    @Transactional(readOnly = true)
    public List<CorrectionResponse> correctionsEnAttente() {
        return correctionRepository.findByStatutOrderByDateDemande(DemandeCorrection.Statut.EN_ATTENTE_DAF).stream()
                .filter(d -> currentUser.peutVoirProjet(d.getEncaissement().getProjetCode()))
                .map(CorrectionResponse::of).toList();
    }

    Encaissement trouver(Long id) {
        Encaissement e = encaissementRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Encaissement introuvable : " + id));
        currentUser.verifierAccesProjet(e.getProjetCode());
        return e;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
