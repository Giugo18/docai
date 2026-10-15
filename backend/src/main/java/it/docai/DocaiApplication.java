package it.docai;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.integration.Slf4jThreadLocalAccessor;
import it.docai.config.IdRichiestaFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class DocaiApplication {

    public static void main(String[] args) {
        // Reactor deve sapere che l'MDC (id della richiesta) va portato da un thread all'altro
        ContextRegistry.getInstance().registerThreadLocalAccessor(
                new Slf4jThreadLocalAccessor(IdRichiestaFilter.CHIAVE_MDC));
        SpringApplication.run(DocaiApplication.class, args);
    }
}
