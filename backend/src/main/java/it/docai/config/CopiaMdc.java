package it.docai.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/** Porta l'MDC (id della richiesta) dal thread che lancia un compito @Async al thread del pool che lo esegue. */
public class CopiaMdc implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable compito) {
        Map<String, String> mdc = MDC.getCopyOfContextMap();     // letto ADESSO, sul thread della richiesta
        return () -> {
            if (mdc != null) {
                MDC.setContextMap(mdc);                          // applicato DOPO, sul thread del pool
            }
            try {
                compito.run();
            } finally {
                MDC.clear();                                     // il thread torna nel pool pulito
            }
        };
    }
}