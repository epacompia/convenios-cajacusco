package org.canalesCMAC.adapter.out.universidadcusco;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class UCuscoAdapter {

    @Inject
    UCuscoSoapMessageCodec codec;

    @Inject
    ProducerTemplate producerTemplate;

    @ConfigProperty(name = "recaudacion.universidadcusco.url")
    String url;

    public RecaudacionResponse procesar(RecaudacionRequest peticion) {

        String soap = codec.serializar(peticion);

        String respuesta = producerTemplate.requestBodyAndHeaders(
            url,
            soap,
            Map.of(
                Exchange.CONTENT_TYPE,
                "text/xml; charset=utf-8"
            ),
            String.class
        );

        return codec.deserializar(
            respuesta,
            peticion
        );
    }
}
