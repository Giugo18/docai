package it.docai.admin;

import it.docai.documento.DocumentoService;
import it.docai.documento.RigaStatistica;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final DocumentoService service;

    public AdminController(DocumentoService service) {
        this.service = service;
    }

    @GetMapping("/statistiche")
    public List<RigaStatistica> statistiche() {
        return service.statistiche();
    }
}