package br.com.hahn.toxicbet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Slf4j
@EnableScheduling
public class ToxicBetApplication implements ApplicationListener<ApplicationReadyEvent> {


    public static void main(String[] args) {
        SpringApplication.run(ToxicBetApplication.class, args);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();

        String[] activeProfiles = env.getActiveProfiles();
        String profile = activeProfiles.length > 0 ? String.join(",", activeProfiles) : "default";

        String port = env.getProperty("server.port", "8080");

        String r2dbcUrl = env.getProperty("spring.r2dbc.url",
                env.getProperty("spring.datasource.url", "n/a"));

        String database = extractDatabaseFromUrl(r2dbcUrl);

        log.info("Perfil Spring definido: {}", profile);
        log.info("Porta usada (configurada): {}", port);
        log.info("Banco de dados usado: {} (url={})", database, r2dbcUrl);
    }

    private String extractDatabaseFromUrl(String url) {
        if (url == null || url.isBlank()) return "n/a";
        try {
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash + 1 < url.length()) {
                String db = url.substring(lastSlash + 1);
                int q = db.indexOf('?');
                if (q > -1) db = db.substring(0, q);
                return db;
            }
        } catch (Exception e) {
            log.warn("Falha ao extrair database da URL de conexão: {}", url, e);
        }
        return url;
    }
}
