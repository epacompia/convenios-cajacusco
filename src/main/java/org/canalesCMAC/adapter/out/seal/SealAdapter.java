package org.canalesCMAC.adapter.out.seal;

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
public class SealAdapter implements ConvenioAdapter {

    @Inject
    SealIso8583Codec codec;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConvenioRegistro registro;

    @ConfigProperty(name = "recaudacion.seal.url")
    String url;

    @Override
    public Institucion institucion() {
        return Institucion.SEAL;
    }

    public RecaudacionResponse procesar(RecaudacionRequest peticion) {
        String trama = codec.construir(peticion);
        Map<String, Object> encabezados = new HashMap<>(registro.autenticacion(Institucion.SEAL).encabezados());
        encabezados.put(Exchange.CONTENT_TYPE, "text/plain; charset=ISO-8859-1");
        String respuesta = producerTemplate.requestBodyAndHeaders(url, trama, encabezados, String.class);
        return codec.interpretar(respuesta, peticion);
    }
}
