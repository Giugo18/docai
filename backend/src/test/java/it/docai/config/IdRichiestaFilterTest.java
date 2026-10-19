package it.docai.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IdRichiestaFilterTest {

    private final IdRichiestaFilter filtro = new IdRichiestaFilter();

    @Test
    void nellAsyncDispatchRiusaLoStessoId() throws Exception {
        var richiesta = new MockHttpServletRequest("POST", "/api/chat/stream");
        var risposta = new MockHttpServletResponse();
        List<String> idVisti = new ArrayList<>();
        FilterChain catena = (req, res) -> idVisti.add(MDC.get(IdRichiestaFilter.CHIAVE_MDC));

        filtro.doFilter(richiesta, risposta, catena);              // 1. richiesta normale: il controller restituisce il Flux
        richiesta.setDispatcherType(DispatcherType.ASYNC);
        filtro.doFilter(richiesta, risposta, catena);              // 2. async dispatch: il flusso è finito o in errore

        assertThat(idVisti).hasSize(2);
        assertThat(idVisti.get(0)).isNotNull();
        assertThat(idVisti.get(1)).isEqualTo(idVisti.get(0));     // stesso id, non uno nuovo
        assertThat(risposta.getHeader(IdRichiestaFilter.HEADER)).isEqualTo(idVisti.get(0));
        assertThat(MDC.get(IdRichiestaFilter.CHIAVE_MDC)).isNull(); // il thread resta pulito
    }

    @Test
    void usaLIdRicevutoSeValido() throws Exception {
        var richiesta = new MockHttpServletRequest("GET", "/api/documenti");
        richiesta.addHeader(IdRichiestaFilter.HEADER, "abc-123");
        var risposta = new MockHttpServletResponse();
        List<String> idVisti = new ArrayList<>();

        filtro.doFilter(richiesta, risposta, (req, res) -> idVisti.add(MDC.get(IdRichiestaFilter.CHIAVE_MDC)));

        assertThat(idVisti).containsExactly("abc-123");
        assertThat(risposta.getHeader(IdRichiestaFilter.HEADER)).isEqualTo("abc-123");
    }
}