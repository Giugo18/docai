package it.docai.estrazione;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

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
}