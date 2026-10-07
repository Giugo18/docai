package it.docai.demo;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dev")
public class DemoController {

    @GetMapping("/api/demo/lento")
    public String lento() throws InterruptedException {
        Thread.sleep(1000);                         // simula un'attesa: DB lento, chiamata all'AI...
        return Thread.currentThread().toString();   // chi ha eseguito la richiesta
    }
}
