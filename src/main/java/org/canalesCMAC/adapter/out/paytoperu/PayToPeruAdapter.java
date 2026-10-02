package org.canalesCMAC.adapter.out.paytoperu;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class PayToPeruAdapter {

@Inject
PayToPeruCodec codec;

@Inject
ProducerTemplate producerTemplate;

@ConfigProperty(name = "recaudacion.paytoperu.url")
String url;

public RecaudacionResponse procesar(RecaudacionRequest peticion) {

    System.out.println("json::" + peticion);
    System.out.println("url::" + url);

    String requestJson = codec.serializar(peticion);

    System.out.println("request::" + requestJson);

    String respuesta = producerTemplate.requestBodyAndHeaders(
        url,
        requestJson,
        Map.of(
            Exchange.CONTENT_TYPE,
            "application/json"
        ),
        String.class
    );

    System.out.println("response::" + respuesta);

    return codec.deserializar(
        respuesta,
        peticion
    );
}

}