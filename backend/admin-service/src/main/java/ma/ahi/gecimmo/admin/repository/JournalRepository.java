package ma.ahi.gecimmo.admin.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import ma.ahi.gecimmo.admin.domain.JournalActivite;

public interface JournalRepository extends JpaRepository<JournalActivite, Long> {
    Page<JournalActivite> findByEvenementStartingWith(String prefixe, Pageable pageable);
}
