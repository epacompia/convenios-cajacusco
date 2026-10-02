package org.canalesCMAC.adapter.out.claro;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ClaroAdapter {

    @Inject
    ClaroSoapMessageCodec codec;

    @Inject
    ProducerTemplate producerTemplate;

    @ConfigProperty(name = "recaudacion.claro.url")
    String url;
    public RecaudacionResponse procesar(RecaudacionRequest peticion) {
        System.out.print("json::" + peticion);
        String soap = codec.serializar(peticion);
        System.out.print("url::" + url);
        System.out.print("soap::" + soap);
        String respuesta = producerTemplate.requestBodyAndHeaders(
            url,
            soap,
            Map.of(Exchange.CONTENT_TYPE,"text/xml; charset=utf-8"),
            String.class
        );

        return codec.deserializar(
            respuesta,
            peticion
        );
    }
}
