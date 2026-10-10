package it.docai.documento;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

@Component
public class ValidatoreUpload {

    static final long DIMENSIONE_MASSIMA = 10 * 1024 * 1024;               // 10 MB
    static final Set<String> TIPI_AMMESSI = Set.of("application/pdf");
    private static final byte[] FIRMA_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);


    public Optional<ErroreUpload> valida(MultipartFile file) {

        if (file.isEmpty()) {
            return Optional.of(new ErroreUpload.FileVuoto());
        }

        var tipo = file.getContentType();
        if (tipo == null || !TIPI_AMMESSI.contains(tipo)) {
            return Optional.of(new ErroreUpload.TipoNonAmmesso(tipo != null ? tipo : "sconosciuto"));
        }

        var dimensione = file.getSize();
        if (dimensione > DIMENSIONE_MASSIMA) {
            return Optional.of(new ErroreUpload.TroppoGrande(dimensione, DIMENSIONE_MASSIMA));
        }

        // il Content-Type lo dichiara il browser in base al nome: il contenuto invece non mente
        if (!iniziaConFirmaPdf(file)) {
            return Optional.of(new ErroreUpload.NonUnPdf());
        }

        return Optional.empty();
    }

    private static boolean iniziaConFirmaPdf(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return Arrays.equals(in.readNBytes(FIRMA_PDF.length), FIRMA_PDF);
        } catch (IOException e) {
            return false;
        }
    }
}
