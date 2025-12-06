package com.auditlog.audit_log.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Konfigurácia globálneho spracovania chýb pre Kafka Consumerov.
 * <p>
 * Táto trieda definuje stratégiu, ako sa má aplikácia zachovať, keď nastane chyba
 * pri spracovaní správy z Kafky (napr. výpadok databázy, zlý formát správy).
 * <p>
 * Zahŕňa:
 * <ul>
 * <li>Exponenciálne opakovanie pokusov (Retry Policy).</li>
 * <li>Dead Letter Queue (DLT) mechanizmus pre neúspešné správy.</li>
 * <li>Filtrovanie fatálnych výnimiek, ktoré sa nemajú opakovať.</li>
 * </ul>
 */
@Configuration
public class KafkaErrorHandlingConfig {

    /**
     * Vytvára a konfiguruje {@link DefaultErrorHandler}, ktorý nahrádza predvolený mechanizmus Spring Kafky.
     *
     * @param kafkaTemplate Template potrebný na odoslanie chybnej správy do DLT topicu.
     * @return Nakonfigurovaný error handler.
     */
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {

// 1. Recoverer: Čo sa stane, keď vyčerpáme všetky pokusy o opakovanie?
        // DeadLetterPublishingRecoverer pošle správu do špeciálneho topicu (napr. employee-events.DLT).
        // To nám umožní neskôr analyzovať chybu bez toho, aby sme blokovali spracovanie ďalších správ.
        DefaultErrorHandler handler = getDefaultErrorHandler(kafkaTemplate);

        // 3. Not Retryable Exceptions: Fatálne chyby, ktoré nemá zmysel opakovať.
        // Ak nastane jedna z týchto chýb, Retry sa preskočí a správa ide OKAMŽITE do DLT.
        handler.addNotRetryableExceptions(
                // Ak porušíme integritu DB (napr. duplicita ID), opakovaním sa to neopraví.
                DataIntegrityViolationException.class,
                // Ak je JSON poškodený alebo má zlý formát, deserializácia nikdy neprejde.
                JsonProcessingException.class
        );

        return handler;
    }

    private static DefaultErrorHandler getDefaultErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);

        // 2. Backoff Policy:, Ako často a koľkokrát máme skúsiť spracovanie znova?
        // Používame exponenciálny backoff, aby sme nepreťažili systém okamžitými opakovaniami.
        // Stratégia:
        // - Max 3 pokusy o opakovanie (spolu 4 pokusy vrátane prvého).
        ExponentialBackOffWithMaxRetries backoff = new ExponentialBackOffWithMaxRetries(3);
        backoff.setInitialInterval(1000L); // Prvý retry po 1 sekunde
        backoff.setMultiplier(2.0);        // Každý ďalší interval sa zdvojnásobí (1s -> 2s -> 4s)

        // Spojenie recoverera a backoff stratégie do handlera
        return new DefaultErrorHandler(recoverer, backoff);
    }
}