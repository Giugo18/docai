package it.docai.rag;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversazioneRepository extends JpaRepository<Conversazione, UUID> {
    List<Conversazione> findByProprietarioOrderByAggiornataIlDesc(String proprietario);
    Optional<Conversazione> findByIdAndProprietario(UUID id, String proprietario);
}
