package ma.ahi.gecimmo.admin.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ma.ahi.gecimmo.admin.domain.JournalActivite;
import ma.ahi.gecimmo.admin.repository.JournalRepository;
import ma.ahi.gecimmo.admin.web.dto.AdminDtos.JournalResponse;

@Service
@Transactional
public class JournalService {

    private final JournalRepository journalRepository;

    public JournalService(JournalRepository journalRepository) {
        this.journalRepository = journalRepository;
    }

    public void tracer(String evenement, String details) {
        journalRepository.save(new JournalActivite(evenement, details));
    }

    @Transactional(readOnly = true)
    public Page<JournalResponse> consulter(String prefixe, Pageable pageable) {
        Page<JournalActivite> page = prefixe == null || prefixe.isBlank()
                ? journalRepository.findAll(pageable)
                : journalRepository.findByEvenementStartingWith(prefixe, pageable);
        return page.map(JournalResponse::of);
    }
}
