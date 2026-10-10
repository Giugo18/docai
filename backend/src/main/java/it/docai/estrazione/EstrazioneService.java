package it.docai.estrazione;

import it.docai.documento.ContenutoDocumentoRepository;
import it.docai.documento.DocumentoNonTrovatoException;
import it.docai.documento.DocumentoRepository;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;

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
    private final VerificaFattura verifica;
    private static final Logger log = LoggerFactory.getLogger(EstrazioneService.class);
    private static final int TENTATIVI_MASSIMI = 2;

    public EstrazioneService(ChatClient.Builder builder,
                             DocumentoRepository documentoRepository,
                             ContenutoDocumentoRepository contenutoRepository,
                             TestoPdf testoPdf,
                             VerificaFattura verifica) {
        this.chatClient = builder.defaultSystem(ISTRUZIONI).build();
        this.documentoRepository = documentoRepository;
        this.contenutoRepository = contenutoRepository;
        this.testoPdf = testoPdf;
        this.verifica = verifica;
    }

    public EsitoEstrazione daTesto(String testo) {
        Fattura fattura = chiedi(testo, List.of());
        List<String> problemi = verifica.problemi(fattura);
        int tentativi = 1;

        while (!problemi.isEmpty() && tentativi < TENTATIVI_MASSIMI) {
            log.info("Estrazione fattura, tentativo {}: {}", tentativi, problemi);
            fattura = chiedi(testo, problemi);      // il modello riceve i propri errori
            problemi = verifica.problemi(fattura);
            tentativi++;
        }
        return new EsitoEstrazione(fattura, problemi, tentativi);
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

    public EsitoEstrazione daDocumento(UUID id, String proprietario) {
        return daTesto(testoDelDocumento(id, proprietario));
    }

    private Fattura chiedi(String testo, List<String> problemiPrecedenti) {
        String messaggio = problemiPrecedenti.isEmpty()
                ? testo
                : testo + "\n\nATTENZIONE: un'estrazione precedente di questa fattura conteneva questi errori. "
                + "Rileggi il testo e correggili:\n- " + String.join("\n- ", problemiPrecedenti);

        return chatClient.prompt()
                .options(ChatOptions.builder().temperature(0.0))   // estrazione: niente creatività
                .user(messaggio)
                .call()
                .entity(Fattura.class);
    }
}