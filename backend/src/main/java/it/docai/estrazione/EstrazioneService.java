package it.docai.estrazione;

import it.docai.documento.ContenutoDocumentoRepository;
import it.docai.documento.DocumentoNonTrovatoException;
import it.docai.documento.DocumentoRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EstrazioneService {

    private static final String ISTRUZIONI = """
            Sei un estrattore di dati da fatture italiane.
            Leggi il testo della fattura e compila i campi richiesti.
            Usa solo informazioni presenti nel testo: se un dato manca, lascia il campo a null.
            Gli importi sono numeri con il punto come separatore decimale (1234.56), senza simbolo di valuta.
            Le date sono nel formato AAAA-MM-GG; nel testo sono in formato italiano GG/MM/AAAA.
            """;

    private final ChatClient chatClient;
    private final DocumentoRepository documentoRepository;
    private final ContenutoDocumentoRepository contenutoRepository;
    private final TestoPdf testoPdf;

    public EstrazioneService(ChatClient.Builder builder,
                             DocumentoRepository documentoRepository,
                             ContenutoDocumentoRepository contenutoRepository,
                             TestoPdf testoPdf) {
        this.chatClient = builder.defaultSystem(ISTRUZIONI).build();
        this.documentoRepository = documentoRepository;
        this.contenutoRepository = contenutoRepository;
        this.testoPdf = testoPdf;
    }

    public Fattura daTesto(String testo) {
        return chatClient.prompt().user(testo).call().entity(Fattura.class);
    }

    /** Il testo estratto dal PDF di un documento dell'utente */
    public String testoDelDocumento(UUID id, String proprietario) {
        documentoRepository.findByIdAndProprietario(id, proprietario)        // protezione IDOR: solo i propri
                .orElseThrow(() -> new DocumentoNonTrovatoException(id));
        var contenuto = contenutoRepository.findById(id)
                .orElseThrow(() -> new ContenutoNonDisponibileException(id));
        String testo = testoPdf.estrai(contenuto.getDati());
        if (testo.isBlank()) {
            throw new TestoNonEstraibileException("Il PDF non contiene testo selezionabile: forse è una scansione");
        }
        return testo;
    }

    public Fattura daDocumento(UUID id, String proprietario) {
        return daTesto(testoDelDocumento(id, proprietario));
    }
}