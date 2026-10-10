package it.docai.sperimentale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/sperimentale")
public class EmbeddingController {

    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;
    public record DaSalvare(@NotBlank String testo) {}
    public record Trovato(String testo, Double punteggio) {}

    public EmbeddingController(EmbeddingModel embeddingModel, VectorStore vectorStore) {
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
    }

    @PostMapping("/similarita")
    Similarita similarita(@Valid @RequestBody CoppiaDiFrasi coppia) {
        float[] a = embeddingModel.embed(coppia.a());
        float[] b = embeddingModel.embed(coppia.b());
        return new Similarita(a.length, coseno(a, b), Arrays.copyOf(a, 5));
    }

    /** cos(θ) = (a·b) / (|a|·|b|) */
    private static double coseno(float[] a, float[] b) {
        double prodotto = 0, normaA = 0, normaB = 0;
        for (int i = 0; i < a.length; i++) {
            prodotto += a[i] * b[i];
            normaA += a[i] * a[i];
            normaB += b[i] * b[i];
        }
        return prodotto / (Math.sqrt(normaA) * Math.sqrt(normaB));
    }

    @PostMapping("/frasi")
    void salva(@Valid @RequestBody DaSalvare richiesta) {
        vectorStore.add(List.of(new Document(richiesta.testo())));
    }

    @GetMapping("/cerca")
    List<Trovato> cerca(@RequestParam String q, @RequestParam(defaultValue = "3") int k) {
        return vectorStore.similaritySearch(SearchRequest.builder().query(q).topK(k).build())
                .stream()
                .map(d -> new Trovato(d.getText(), d.getScore()))
                .toList();
    }
}