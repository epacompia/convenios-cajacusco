package org.canalesCMAC.adapter.out.electroucayali;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ElectroUcayaliToken {

    private static final Duration TIEMPO_MAXIMO = Duration.ofHours(24);

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "recaudacion.electroucayali.url")
    String urlBase;

    @ConfigProperty(name = "recaudacion.electroucayali.usuario")
    String usuario;

    @ConfigProperty(name = "recaudacion.electroucayali.clave")
    String clave;

    private volatile String token;
    private volatile Instant vence;

    public String obtener() {
        String actual = token;
        if (actual != null && Instant.now().isBefore(vence)) {
            return actual;
        }
        synchronized (this) {
            if (token != null && Instant.now().isBefore(vence)) {
                return token;
            }
            autenticar();
            return token;
        }
    }

    private void autenticar() {
        try {
            String cuerpo = objectMapper.writeValueAsString(Map.of("Usuario", usuario, "Clave", clave));
            String respuesta = producerTemplate.requestBodyAndHeaders(
                urlBase + "/api/v1/electro/auth",
                cuerpo,
                Map.of(Exchange.CONTENT_TYPE, "application/json"),
                String.class);
            JsonNode nodo = objectMapper.readTree(respuesta);
            String nuevoToken = nodo.path("token").asText();
            if (nuevoToken.isBlank()) {
                throw new IllegalStateException("Electro Ucayali no otorgó token: " + respuesta);
            }
            token = nuevoToken;
            long expira = nodo.hasNonNull("Expires_in") ? Long.parseLong(nodo.get("Expires_in").asText()) : TIEMPO_MAXIMO.toSeconds();
            vence = Instant.now().plus(Duration.ofSeconds(expira));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo autenticar en Electro Ucayali: " + e.getMessage(), e);
        }
    }
}