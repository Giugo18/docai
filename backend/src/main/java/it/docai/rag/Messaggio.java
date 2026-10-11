package it.docai.rag;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messaggio")
public class Messaggio {

    @Id
    private UUID id;

    @Column(name = "conversazione_id", nullable = false)
    private UUID conversazioneId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuoloMessaggio ruolo;

    @Column(nullable = false)
    private String testo;

    private String fonti;   // JSON

    @Column(name = "creato_il", nullable = false)
    private Instant creatoIl;

    protected Messaggio() {
        // richiesto da JPA
    }

    public static Messaggio dellUtente(UUID conversazioneId, String testo) {
        return nuovo(conversazioneId, RuoloMessaggio.UTENTE, testo, null);
    }

    public static Messaggio dellAssistente(UUID conversazioneId, String testo, String fontiJson) {
        return nuovo(conversazioneId, RuoloMessaggio.ASSISTENTE, testo, fontiJson);
    }

    private static Messaggio nuovo(UUID conversazioneId, RuoloMessaggio ruolo, String testo, String fonti) {
        var m = new Messaggio();
        m.id = UUID.randomUUID();
        m.conversazioneId = conversazioneId;
        m.ruolo = ruolo;
        m.testo = testo;
        m.fonti = fonti;
        m.creatoIl = Instant.now();
        return m;
    }

    public UUID getId()               { return id; }
    public UUID getConversazioneId()  { return conversazioneId; }
    public RuoloMessaggio getRuolo()  { return ruolo; }
    public String getTesto()          { return testo; }
    public String getFonti()          { return fonti; }
    public Instant getCreatoIl()      { return creatoIl; }
}