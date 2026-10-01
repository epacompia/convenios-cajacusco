package org.canalesCMAC.adapter.out.electrosureste;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.canalesCMAC.application.ConvenioRegistro;
import org.canalesCMAC.application.port.ConvenioAdapter;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ElseAdapter implements ConvenioAdapter {

    @Inject
    ElseSoapMessageCodec codec;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConvenioRegistro registro;

    @ConfigProperty(name = "recaudacion.electrosureste.url")
    String url;

    @Override
    public Institucion institucion() {
        return Institucion.ELSE;
    }

    public RecaudacionResponse procesar(RecaudacionRequest peticion) {
        String soap = codec.serializar(peticion);
        Map<String, Object> encabezados = new HashMap<>(registro.autenticacion(Institucion.ELSE).encabezados());
        encabezados.put(Exchange.CONTENT_TYPE, "text/xml; charset=utf-8");
        String respuesta = producerTemplate.requestBodyAndHeaders(url, soap, encabezados, String.class);
        return codec.deserializar(respuesta, peticion);
    }
}