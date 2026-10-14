package it.docai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DocaiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocaiApplication.class, args);
    }
}
