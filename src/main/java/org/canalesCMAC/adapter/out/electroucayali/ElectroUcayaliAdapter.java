package org.canalesCMAC.adapter.out.electroucayali;

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
public class ElectroUcayaliAdapter implements ConvenioAdapter {

    @Inject
    ElectroUcayaliConvertidor convertidor;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConvenioRegistro registro;

    @ConfigProperty(name = "recaudacion.electroucayali.url")
    String urlBase;

    @Override
    public Institucion institucion() {
        return Institucion.ELECTRO_UCAYALI;
    }

    public RecaudacionResponse procesar(RecaudacionRequest peticion) {
        ElectroUcayaliConvertidor.Solicitud solicitud = convertidor.construir(peticion);
        Map<String, Object> encabezados = new HashMap<>(registro.autenticacion(Institucion.ELECTRO_UCAYALI).encabezados());
        encabezados.put(Exchange.CONTENT_TYPE, "application/json");
        String respuesta = producerTemplate.requestBodyAndHeaders(
            urlBase + solicitud.ruta(), solicitud.cuerpo(), encabezados, String.class);
        return convertidor.interpretar(respuesta, peticion);
    }
}