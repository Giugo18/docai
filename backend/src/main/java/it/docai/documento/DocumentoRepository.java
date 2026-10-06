package it.docai.documento;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentoRepository extends JpaRepository<Documento, UUID> {

    List<Documento> findByProprietarioOrderByCaricatoIlDesc(String proprietario);
}
