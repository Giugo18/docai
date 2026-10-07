package it.docai.documento;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentoService {

    private final DocumentoRepository repository;

    public DocumentoService(DocumentoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DocumentoDto carica(MultipartFile file, String proprietario) {
        var documento = Documento.nuovo(file.getOriginalFilename(), file.getContentType() != null ? file.getContentType() : "application/octet-stream", file.getSize(), proprietario);
        repository.save(documento);

        // TODO settimana 8: pipeline di ingestione
        //   1. leggi il contenuto con TikaDocumentReader
        //   2. spezza in chunk con TokenTextSplitter
        //   3. aggiungi ai metadati di ogni chunk "proprietario" e "documentoId"
        //   4. salva con vectorStore.add(chunks) e chiama documento.segnaIndicizzato()
        // TODO settimana 12: spostala in un processo asincrono (virtual thread)

        return DocumentoDto.da(documento);
    }

    @Transactional(readOnly = true)
    public List<DocumentoDto> elenca(String proprietario) {
        return repository.findByProprietarioOrderByCaricatoIlDesc(proprietario).stream().map(DocumentoDto::da).toList();
    }

    @Transactional(readOnly = true)
    public RiepilogoDocumenti riepilogo(String proprietario) {
        // TODO:

        /*var documenti = repository.findByProprietarioOrderByCaricatoIlDesc(proprietario);
        long totale = documenti.size();
        long dimensioneTotale = documenti.stream()
                .mapToLong(Documento::getDimensione)
                .sum();

        var perStato = documenti.stream().collect(Collectors.groupingBy(
                Documento::getStato,
                Collectors.counting()));

        return new RiepilogoDocumenti(totale, dimensioneTotale, perStato);*/

        var righe = repository.riepilogoPerStato(proprietario);

        long totale = righe.stream()
                .mapToLong(RigaRiepilogo::getNumero)
                .sum();
        long dimensioneTotale = righe.stream()
                .mapToLong(RigaRiepilogo::getDimensione)
                .sum();
        var perStato = righe.stream()
                .collect(Collectors.toMap(
                        r -> StatoDocumento.valueOf(r.getStato()),
                        RigaRiepilogo::getNumero));

        return new RiepilogoDocumenti(totale, dimensioneTotale, perStato);

    }

    @Transactional(readOnly = true)
    public DocumentoDto trova(UUID id, String proprietario) {
        // TODO: findByIdAndProprietario → map(DocumentoDto::da) → orElseThrow(...)
        return repository.findByIdAndProprietario(id, proprietario)   // Optional<Documento>
                .map(DocumentoDto::da)                                // Optional<DocumentoDto>
                .orElseThrow(() -> new DocumentoNonTrovatoException(id));
    }
}
