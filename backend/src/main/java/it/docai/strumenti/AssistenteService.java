package it.docai.strumenti;

import it.docai.rag.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AssistenteService {

    private final ChatClient chatClient;
    private final StrumentiDocumenti strumentiDocumenti;
    private final String regole;
    private final StrumentiRicerca strumentiRicerca;   // nel costruttore, come strumentiDocumenti
    private final ConversazioneService conversazioni;
    private final StrumentiModifica strumentiModifica;

    public AssistenteService(ChatClient chatClient, StrumentiDocumenti strumentiDocumenti,
                             @Value("classpath:prompts/strumenti.st") Resource prompt,
                             StrumentiRicerca strumentiRicerca,
                             ConversazioneService conversazioni, StrumentiModifica strumentiModifica) throws IOException {
        this.chatClient = chatClient;
        this.strumentiDocumenti = strumentiDocumenti;
        this.regole = prompt.getContentAsString(StandardCharsets.UTF_8);
        this.strumentiRicerca = strumentiRicerca;
        this.conversazioni = conversazioni;
        this.strumentiModifica = strumentiModifica;
    }

    public String chiedi(String domanda, String proprietario) {
        String risposta = chatClient.prompt()
                .system(regole)
                .user(domanda)
                .tools(strumentiDocumenti, strumentiRicerca, strumentiModifica)
                .toolContext(ContestoStrumenti.di(proprietario, null, new ArrayList<>(), new ArrayList<>()))
                .call()
                .content();
        return Citazioni.normalizza(risposta);
    }

    public Flux<EventoRag> rispondiInConversazione(UUID conversazioneId, String domanda, String proprietario) {
        Conversazione c = conversazioni.trova(conversazioneId, proprietario);      // 404 se non è mia
        List<Messaggio> storia = conversazioni.ultimi(c.getId());                  // PRIMA di salvare la nuova
        conversazioni.registraDomanda(c.getId(), domanda);

        var fonti = new CopyOnWriteArrayList<Fonte>();                             // la riempiono i tool
        var risposta = new StringBuilder();
        var proposte = new CopyOnWriteArrayList<Proposta>();

        Flux<EventoRag> testo = chatClient.prompt()
                .system(regole)
                .messages(cronologia(storia))                                      // la conversazione finora
                .user(domanda)
                .tools(strumentiDocumenti, strumentiRicerca, strumentiModifica)
                .toolContext(ContestoStrumenti.di(proprietario, c.getDocumentoId(), fonti, proposte))
                .stream()
                .content()
                .doOnNext(risposta::append)
                .map(EventoRag::testo);

        Flux<EventoRag> fine = Flux.defer(() -> {
            String completa = Citazioni.normalizza(risposta.toString());
            conversazioni.registraRisposta(c.getId(), completa, List.copyOf(fonti));
            return Flux.concat(
                    Flux.just(EventoRag.fonti(List.copyOf(fonti))),
                    Flux.fromIterable(List.copyOf(proposte)).map(EventoRag::proposta));
        });

        return Flux.concat(testo, fine);
    }

    private static List<Message> cronologia(List<Messaggio> storia) {
        return storia.stream()
                .<Message>map(m -> m.getRuolo() == RuoloMessaggio.UTENTE
                        ? new UserMessage(m.getTesto())
                        : new AssistantMessage(m.getTesto()))
                .toList();
    }
}