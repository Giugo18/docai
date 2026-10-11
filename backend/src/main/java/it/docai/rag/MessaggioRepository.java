package it.docai.rag;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessaggioRepository extends JpaRepository<Messaggio, UUID> {
    List<Messaggio> findByConversazioneIdOrderByCreatoIl(UUID conversazioneId);
    List<Messaggio> findTop6ByConversazioneIdOrderByCreatoIlDesc(UUID conversazioneId);

}