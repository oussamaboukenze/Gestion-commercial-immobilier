package ma.ahi.gecimmo.vente.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import ma.ahi.gecimmo.vente.domain.PieceJointe;
import ma.ahi.gecimmo.vente.domain.Reservation;
import ma.ahi.gecimmo.vente.exception.BusinessException;
import ma.ahi.gecimmo.vente.repository.PieceJointeRepository;
import ma.ahi.gecimmo.vente.security.CurrentUser;

/** Upload / téléchargement des pièces du dossier (PDF, JPEG, PNG ; 10 Mo max). */
@Service
@Transactional
public class PieceService {

    public record PieceResponse(Long id, Long reservationId, PieceJointe.Type type, String nomFichier,
                                String contentType, long taille, java.time.LocalDateTime dateDepot,
                                String deposePar) {
        public static PieceResponse of(PieceJointe p) {
            return new PieceResponse(p.getId(), p.getReservationId(), p.getType(), p.getNomFichier(),
                    p.getContentType(), p.getTaille(), p.getDateDepot(), p.getDeposePar());
        }
    }

    public record Fichier(Resource resource, String nom, String contentType) {
    }

    private static final Set<String> TYPES_AUTORISES = Set.of("application/pdf", "image/jpeg", "image/png");

    private final PieceJointeRepository pieceRepository;
    private final ReservationService reservationService;
    private final CurrentUser currentUser;
    private final Path racine;

    public PieceService(PieceJointeRepository pieceRepository, ReservationService reservationService,
                        CurrentUser currentUser, @Value("${gecimmo.stockage.dossier:./data/pieces}") String dossier) {
        this.pieceRepository = pieceRepository;
        this.reservationService = reservationService;
        this.currentUser = currentUser;
        this.racine = Path.of(dossier).toAbsolutePath().normalize();
    }

    public PieceResponse deposer(Long reservationId, PieceJointe.Type type, MultipartFile fichier) {
        Reservation r = reservationService.trouverAvecControle(reservationId);
        if (fichier == null || fichier.isEmpty()) {
            throw new BusinessException("Fichier vide.");
        }
        String contentType = fichier.getContentType() == null ? "" : fichier.getContentType().toLowerCase(Locale.ROOT);
        if (!TYPES_AUTORISES.contains(contentType)) {
            throw new BusinessException("Format refusé : PDF, JPEG ou PNG uniquement.");
        }
        String original = fichier.getOriginalFilename() == null ? "piece" : Path.of(fichier.getOriginalFilename())
                .getFileName().toString();
        String extension = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        String relatif = r.getId() + "/" + type.name().toLowerCase(Locale.ROOT) + "-" + UUID.randomUUID() + extension;
        Path cible = racine.resolve(relatif).normalize();
        if (!cible.startsWith(racine)) {
            throw new BusinessException("Nom de fichier invalide.");
        }
        try (InputStream in = fichier.getInputStream()) {
            Files.createDirectories(cible.getParent());
            Files.copy(in, cible, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Enregistrement du fichier impossible.");
        }
        PieceJointe piece = pieceRepository.save(new PieceJointe(r.getId(), type, original, contentType,
                fichier.getSize(), relatif, currentUser.username()));
        // La case "pièce fournie" se coche automatiquement
        r.setPieces(r.isPieceCin() || type == PieceJointe.Type.CIN,
                r.isPieceJustificatifRevenus() || type == PieceJointe.Type.JUSTIFICATIF_REVENUS,
                r.isPieceRecuAvance() || type == PieceJointe.Type.RECU_AVANCE);
        return PieceResponse.of(piece);
    }

    @Transactional(readOnly = true)
    public List<PieceResponse> lister(Long reservationId) {
        reservationService.trouverAvecControle(reservationId);
        return pieceRepository.findByReservationIdOrderByDateDepot(reservationId).stream()
                .map(PieceResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public Fichier telecharger(Long pieceId) {
        PieceJointe p = pieceRepository.findById(pieceId)
                .orElseThrow(() -> BusinessException.notFound("Pièce introuvable : " + pieceId));
        reservationService.trouverAvecControle(p.getReservationId());
        Path fichier = racine.resolve(p.getChemin()).normalize();
        if (!fichier.startsWith(racine) || !Files.exists(fichier)) {
            throw BusinessException.notFound("Fichier absent du stockage.");
        }
        return new Fichier(new FileSystemResource(fichier), p.getNomFichier(), p.getContentType());
    }
}
