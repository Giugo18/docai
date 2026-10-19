package it.docai.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Dà a ogni richiesta un id che finisce in tutte le righe di log (MDC) e nell'header di risposta.
 * Se il chiamante manda già X-Request-Id valido, si usa quello: così si segue una richiesta tra più servizi.
 *
 * Streaming (Flux, SSE): il controller esce subito e la risposta, o l'errore, arriva dopo con una
 * "async dispatch" su un altro thread. L'MDC è legato al thread, quindi lì sarebbe vuoto: per questo
 * l'id si salva anche come attributo della richiesta (che segue la richiesta, non il thread) e il filtro
 * gira anche nell'async dispatch, dove riusa quell'id invece di crearne uno nuovo.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)          // prima di tutto, anche di Spring Security: anche un 401 ha il suo id
public class IdRichiestaFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String CHIAVE_MDC = "idRichiesta";
    public static final String ATTRIBUTO = IdRichiestaFilter.class.getName() + ".id";
    private static final Pattern VALIDO = Pattern.compile("[A-Za-z0-9-]{1,64}");

    /** false = il filtro gira anche nell'async dispatch (di default OncePerRequestFilter la salta). */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest richiesta, HttpServletResponse risposta, FilterChain catena)
            throws ServletException, IOException {
        String id = (String) richiesta.getAttribute(ATTRIBUTO);      // già presente = siamo nell'async dispatch
        if (id == null) {
            String ricevuto = richiesta.getHeader(HEADER);
            id = ricevuto != null && VALIDO.matcher(ricevuto).matches()
                    ? ricevuto
                    : UUID.randomUUID().toString().substring(0, 8);   // 8 caratteri bastano per distinguere le richieste
            richiesta.setAttribute(ATTRIBUTO, id);
            risposta.setHeader(HEADER, id);                          // solo la prima volta: dopo la risposta può essere già partita
        }
        MDC.put(CHIAVE_MDC, id);
        try {
            catena.doFilter(richiesta, risposta);
        } finally {
            MDC.remove(CHIAVE_MDC);                                   // Tomcat riusa i thread: senza, l'id "contamina" la prossima richiesta
        }
    }
}