package it.docai.estrazione;

import jakarta.validation.Valid;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/estrazione")
public class EstrazioneController {

    private static final String ISTRUZIONI = """
            Sei un estrattore di dati da fatture italiane.
            Leggi il testo della fattura e compila i campi richiesti.
            Usa solo informazioni presenti nel testo: se un dato manca, lascia il campo a null.
            Gli importi sono numeri con il punto come separatore decimale (1234.56), senza simbolo di valuta.
            Le date sono nel formato AAAA-MM-GG; nel testo sono in formato italiano GG/MM/AAAA.
            """;

    private final ChatClient chatClient;

    public EstrazioneController(ChatClient.Builder builder) {
        // un ChatClient diverso da quello della chat: altro compito, altre istruzioni
        this.chatClient = builder.defaultSystem(ISTRUZIONI).build();
    }

    @PostMapping("/fattura")
    public Fattura estraiFattura(@Valid @RequestBody TestoDaEstrarre richiesta) {
        return chatClient.prompt()
                .user(richiesta.testo())
                .call()
                .entity(Fattura.class);   // invece di content(): JSON → record
    }
}