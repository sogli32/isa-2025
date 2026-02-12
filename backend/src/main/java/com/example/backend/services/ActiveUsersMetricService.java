package com.example.backend.services;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servis za pracenje broja aktivnih korisnika u poslednjih 24h.
 * Svaki put kada korisnik napravi autentifikovan zahtev,
 * JwtAuthenticationFilter poziva recordUserActivity().
 * Prometheus scrape-uje Gauge metriku "active_users_24h".
 */
@Service
public class ActiveUsersMetricService {

    // Mapa: email korisnika -> poslednje vreme aktivnosti
    private final Map<String, LocalDateTime> activeUsers = new ConcurrentHashMap<>();

    public ActiveUsersMetricService(MeterRegistry meterRegistry) {
        // Registruj Gauge metriku koja vraca broj aktivnih korisnika u poslednjih 24h
        Gauge.builder("active_users_24h", this, ActiveUsersMetricService::getActiveUserCount)
                .description("Broj aktivnih korisnika u poslednjih 24 sata")
                .register(meterRegistry);
    }

    /**
     * Belezi aktivnost korisnika. Poziva se iz JwtAuthenticationFilter-a
     * svaki put kad korisnik posalje validan JWT token.
     */
    public void recordUserActivity(String userEmail) {
        activeUsers.put(userEmail, LocalDateTime.now());
    }

    /**
     * Vraca broj jedinstvenih korisnika koji su bili aktivni u poslednjih 24h.
     */
    public double getActiveUserCount() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

        // Ocisti stare unose i prebroj aktivne
        activeUsers.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));

        return activeUsers.size();
    }

    /**
     * Brise sve simulirane/test korisnike. Koristi se za reset nakon load testa.
     */
    public void clearAll() {
        activeUsers.clear();
    }
}
