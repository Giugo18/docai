package it.docai.rag;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversazione")
public class Conversazione {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String proprietario;

    @Column(nullable = false)
    private String titolo;

    @Column(name = "documento_id")
    private UUID documentoId;

    @Column(name = "creata_il", nullable = false)
    private Instant creataIl;

    @Column(name = "aggiornata_il", nullable = false)
    private Instant aggiornataIl;

    protected Conversazione() {
        // richiesto da JPA
    }

    public static Conversazione nuova(String proprietario, String titolo, UUID documentoId) {
        var c = new Conversazione();
        c.id = UUID.randomUUID();
        c.proprietario = proprietario;
        c.titolo = titolo;
        c.documentoId = documentoId;
        c.creataIl = Instant.now();
        c.aggiornataIl = c.creataIl;
        return c;
    }

    /** Un nuovo messaggio la porta in cima all'elenco. */
    public void toccata() { this.aggiornataIl = Instant.now(); }

    public UUID getId()             { return id; }
    public String getProprietario() { return proprietario; }
    public String getTitolo()       { return titolo; }
    public UUID getDocumentoId()    { return documentoId; }
    public Instant getCreataIl()    { return creataIl; }
    public Instant getAggiornataIl(){ return aggiornataIl; }
}