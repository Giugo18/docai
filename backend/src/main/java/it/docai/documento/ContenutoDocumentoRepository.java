package it.docai.documento;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ContenutoDocumentoRepository extends JpaRepository<ContenutoDocumento, UUID> {
}