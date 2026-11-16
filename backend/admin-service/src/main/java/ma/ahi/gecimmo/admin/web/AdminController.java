package ma.ahi.gecimmo.admin.web;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ma.ahi.gecimmo.admin.service.JournalService;
import ma.ahi.gecimmo.admin.service.UtilisateurService;
import ma.ahi.gecimmo.admin.web.dto.AdminDtos.CreationResponse;
import ma.ahi.gecimmo.admin.web.dto.AdminDtos.DroitsRequest;
import ma.ahi.gecimmo.admin.web.dto.AdminDtos.JournalResponse;
import ma.ahi.gecimmo.admin.web.dto.AdminDtos.MotDePasseResponse;
import ma.ahi.gecimmo.admin.web.dto.AdminDtos.UtilisateurRequest;
import ma.ahi.gecimmo.admin.web.dto.AdminDtos.UtilisateurResponse;

/** Menu Administration (admin SI). */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN_SI')")
public class AdminController {

    private final UtilisateurService utilisateurService;
    private final JournalService journalService;

    public AdminController(UtilisateurService utilisateurService, JournalService journalService) {
        this.utilisateurService = utilisateurService;
        this.journalService = journalService;
    }

    @GetMapping("/utilisateurs")
    public List<UtilisateurResponse> utilisateurs(@RequestParam(required = false) String q) {
        return utilisateurService.lister(q);
    }

    @GetMapping("/utilisateurs/{id}")
    public UtilisateurResponse utilisateur(@PathVariable String id) {
        return utilisateurService.get(id);
    }

    @PostMapping("/utilisateurs")
    @ResponseStatus(HttpStatus.CREATED)
    public CreationResponse creer(@Valid @RequestBody UtilisateurRequest req) {
        return utilisateurService.creer(req);
    }

    @PutMapping("/utilisateurs/{id}/droits")
    public UtilisateurResponse droits(@PathVariable String id, @Valid @RequestBody DroitsRequest req) {
        return utilisateurService.modifierDroits(id, req);
    }

    @PostMapping("/utilisateurs/{id}/desactiver")
    public UtilisateurResponse desactiver(@PathVariable String id) {
        return utilisateurService.activer(id, false);
    }

    @PostMapping("/utilisateurs/{id}/activer")
    public UtilisateurResponse activer(@PathVariable String id) {
        return utilisateurService.activer(id, true);
    }

    @PostMapping("/utilisateurs/{id}/reinitialiser-mot-de-passe")
    public MotDePasseResponse reinitialiser(@PathVariable String id) {
        return utilisateurService.reinitialiserMotDePasse(id);
    }

    @GetMapping("/projets")
    public List<String> projets() {
        return utilisateurService.projetsDisponibles();
    }

    /** Ex. GET /api/admin/journal?evenement=reservation */
    @GetMapping("/journal")
    public Page<JournalResponse> journal(@RequestParam(required = false) String evenement,
                                         @PageableDefault(size = 50, sort = "date",
                                                 direction = Sort.Direction.DESC) Pageable pageable) {
        return journalService.consulter(evenement, pageable);
    }
}
