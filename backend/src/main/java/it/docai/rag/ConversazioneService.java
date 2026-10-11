package it.docai.rag;

import it.docai.documento.DocumentoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ConversazioneService {

    private static final TypeReference<List<Fonte>> LISTA_FONTI = new TypeReference<>() {};

    private final ConversazioneRepository conversazioni;
    private final MessaggioRepository messaggi;
    private final DocumentoService documenti;
    private final JsonMapper json;

    public ConversazioneService(ConversazioneRepository conversazioni, MessaggioRepository messaggi,
                                DocumentoService documenti, JsonMapper json) {
        this.conversazioni = conversazioni;
        this.messaggi = messaggi;
        this.documenti = documenti;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public List<ConversazioneDto> elenca(String proprietario) {
        return conversazioni.findByProprietarioOrderByAggiornataIlDesc(proprietario).stream()
                .map(ConversazioneDto::da)
                .toList();
    }

    @Transactional
    public ConversazioneDto crea(NuovaConversazione richiesta, String proprietario) {
        if (richiesta.documentoId() != null) {
            documenti.trovaDelProprietario(richiesta.documentoId(), proprietario);   // 404 se non è mio
        }
        var c = Conversazione.nuova(proprietario, richiesta.titolo(), richiesta.documentoId());
        return ConversazioneDto.da(conversazioni.save(c));
    }

    @Transactional(readOnly = true)
    public DettaglioConversazione apri(UUID id, String proprietario) {
        var c = trova(id, proprietario);
        var lista = messaggi.findByConversazioneIdOrderByCreatoIl(c.getId()).stream()
                .map(m -> new MessaggioDto(m.getRuolo(), m.getTesto(), leggiFonti(m.getFonti()), m.getCreatoIl()))
                .toList();
        return new DettaglioConversazione(ConversazioneDto.da(c), lista);
    }

    @Transactional
    public void elimina(UUID id, String proprietario) {
        conversazioni.delete(trova(id, proprietario));   // i messaggi: ON DELETE CASCADE
    }

    Conversazione trova(UUID id, String proprietario) {
        return conversazioni.findByIdAndProprietario(id, proprietario)
                .orElseThrow(() -> new ConversazioneNonTrovataException(id));
    }

    private List<Fonte> leggiFonti(String fontiJson) {
        return fontiJson == null ? List.of() : json.readValue(fontiJson, LISTA_FONTI);
    }

    /** Gli ultimi messaggi, dal più vecchio al più nuovo. */
    @Transactional(readOnly = true)
    public List<Messaggio> ultimi(UUID conversazioneId) {
        var recenti = new ArrayList<>(messaggi.findTop6ByConversazioneIdOrderByCreatoIlDesc(conversazioneId));
        Collections.reverse(recenti);
        return recenti;
    }

    @Transactional
    public void registraDomanda(UUID conversazioneId, String domanda) {
        messaggi.save(Messaggio.dellUtente(conversazioneId, domanda));
        conversazioni.findById(conversazioneId).ifPresent(Conversazione::toccata);
    }

    @Transactional
    public void registraRisposta(UUID conversazioneId, String risposta, List<Fonte> fonti) {
        messaggi.save(Messaggio.dellAssistente(conversazioneId, risposta, json.writeValueAsString(fonti)));
        conversazioni.findById(conversazioneId).ifPresent(Conversazione::toccata);
    }
}