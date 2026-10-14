package it.docai.admin;

import it.docai.consumo.ConsumoService;
import it.docai.consumo.RiepilogoConsumi;
import it.docai.documento.DocumentoService;
import it.docai.documento.RigaStatistica;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final DocumentoService service;
    private final ConsumoService consumoService;

    public AdminController(DocumentoService service, ConsumoService consumoService) {
        this.service = service;
        this.consumoService = consumoService;
    }

    @GetMapping("/statistiche")
    public List<RigaStatistica> statistiche() {
        return service.statistiche();
    }

    @GetMapping("/consumi")
    public RiepilogoConsumi consumi(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mese) {
        return consumoService.riepilogo(mese != null ? mese : YearMonth.now(ZoneId.of("Europe/Rome")));
    }
}