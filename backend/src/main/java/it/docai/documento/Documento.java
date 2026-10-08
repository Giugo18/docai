package it.docai.documento;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documento")
public class Documento {

    @Id
    private UUID id;

    @Column(name = "nome_file", nullable = false)
    private String nomeFile;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long dimensione;

    @Column(nullable = false)
    private String proprietario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatoDocumento stato;

    @Column(name = "caricato_il", nullable = false)
    private Instant caricatoIl;

    protected Documento() {
        // richiesto da JPA
    }

    public static Documento nuovo(String nomeFile, String contentType, long dimensione, String proprietario) {
        var d = new Documento();
        d.id = UUID.randomUUID();
        d.nomeFile = nomeFile;
        d.contentType = contentType;
        d.dimensione = dimensione;
        d.proprietario = proprietario;
        d.stato = StatoDocumento.CARICATO;
        d.caricatoIl = Instant.now();
        return d;
    }

    public void segnaIndicizzato() { this.stato = StatoDocumento.INDICIZZATO; }
    public void segnaErrore()      { this.stato = StatoDocumento.ERRORE; }

    public UUID getId()               { return id; }
    public String getNomeFile()       { return nomeFile; }
    public String getContentType()    { return contentType; }
    public long getDimensione()       { return dimensione; }
    public String getProprietario()   { return proprietario; }
    public StatoDocumento getStato()  { return stato; }
    public Instant getCaricatoIl()    { return caricatoIl; }

    public void rinomina(String nuovoNome) {
        this.nomeFile = nuovoNome;
    }
}
