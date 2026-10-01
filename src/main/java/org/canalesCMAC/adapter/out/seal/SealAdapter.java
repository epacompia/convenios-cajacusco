package org.canalesCMAC.adapter.out.seal;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SealAdapter {

    @Inject
    SealIso8583Codec codec;

    @Inject
    ProducerTemplate producerTemplate;

    @ConfigProperty(name = "recaudacion.seal.url")
    String url;

    public RecaudacionResponse procesar(RecaudacionRequest peticion) {
        String trama = codec.construir(peticion);
        String respuesta = producerTemplate.requestBodyAndHeaders(
            url,
            trama,
            Map.of(Exchange.CONTENT_TYPE, "text/plain; charset=ISO-8859-1"),
            String.class);
        return codec.interpretar(respuesta, peticion);
    }
}
