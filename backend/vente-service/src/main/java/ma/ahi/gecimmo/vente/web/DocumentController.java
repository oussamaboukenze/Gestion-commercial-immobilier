package ma.ahi.gecimmo.vente.web;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ma.ahi.gecimmo.vente.domain.PieceJointe;
import ma.ahi.gecimmo.vente.service.DocumentVenteService;
import ma.ahi.gecimmo.vente.service.PieceService;
import ma.ahi.gecimmo.vente.service.PieceService.Fichier;
import ma.ahi.gecimmo.vente.service.PieceService.PieceResponse;

/** Documents générés + pièces du dossier. */
@RestController
@RequestMapping("/api/ventes")
public class DocumentController {

    private static final String LECTURE = "hasAnyRole('COMMERCIAL','RESP_COMMERCIAL','ADV','COMPTABLE','DAF','DIRECTION')";

    private final DocumentVenteService documentService;
    private final PieceService pieceService;

    public DocumentController(DocumentVenteService documentService, PieceService pieceService) {
        this.documentService = documentService;
        this.pieceService = pieceService;
    }

    @GetMapping(value = "/reservations/{id}/bon-reservation", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize(LECTURE)
    public String bonDeReservation(@PathVariable Long id) {
        return documentService.bonDeReservation(id);
    }

    @GetMapping(value = "/compromis/{id}/document", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize(LECTURE)
    public String compromis(@PathVariable Long id) {
        return documentService.compromisEtEcheancier(id);
    }

    /** Upload : POST multipart, champ "fichier", ?type=CIN|JUSTIFICATIF_REVENUS|RECU_AVANCE|AUTRE */
    @PostMapping(value = "/reservations/{id}/pieces", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('COMMERCIAL','RESP_COMMERCIAL','ADV')")
    public PieceResponse deposer(@PathVariable Long id, @RequestParam PieceJointe.Type type,
                                 @RequestParam("fichier") MultipartFile fichier) {
        return pieceService.deposer(id, type, fichier);
    }

    @GetMapping("/reservations/{id}/pieces")
    @PreAuthorize(LECTURE)
    public List<PieceResponse> pieces(@PathVariable Long id) {
        return pieceService.lister(id);
    }

    @GetMapping("/pieces/{pieceId}/fichier")
    @PreAuthorize(LECTURE)
    public ResponseEntity<Resource> telecharger(@PathVariable Long pieceId) {
        Fichier f = pieceService.telecharger(pieceId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(f.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(f.nom(), StandardCharsets.UTF_8).build().toString())
                .body(f.resource());
    }
}
