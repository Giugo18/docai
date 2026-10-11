package it.docai.estrazione;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class TestoPdf {

    /** Il testo selezionabile del PDF, pagina dopo pagina */
    public String estrai(byte[] pdf) {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(documento);
        } catch (IOException e) {
            throw new TestoNonEstraibileException("Il file non è un PDF leggibile");
        }
    }

    /** Una stringa per pagina; l'indice 0 è la pagina 1. */
    public List<String> perPagina(byte[] pdf) {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            var stripper = new PDFTextStripper();
            int totale = documento.getNumberOfPages();
            var pagine = new ArrayList<String>(totale);
            for (int p = 1; p <= totale; p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                pagine.add(stripper.getText(documento));
            }
            return pagine;
        } catch (IOException e) {
            throw new TestoNonEstraibileException("Impossibile leggere il PDF", e);
        }
    }
}