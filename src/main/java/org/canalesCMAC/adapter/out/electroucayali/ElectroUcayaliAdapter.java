package org.canalesCMAC.adapter.out.electroucayali;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ElectroUcayaliAdapter {

    @Inject
    ElectroUcayaliToken tokenBean;

    @Inject
    ElectroUcayaliConvertidor convertidor;

    @Inject
    ProducerTemplate producerTemplate;

    @ConfigProperty(name = "recaudacion.electroucayali.url")
    String urlBase;

    public RecaudacionResponse procesar(RecaudacionRequest peticion) {
        String token = tokenBean.obtener();
        ElectroUcayaliConvertidor.Solicitud solicitud = convertidor.construir(peticion);
        String respuesta = producerTemplate.requestBodyAndHeaders(
            urlBase + solicitud.ruta(),
            solicitud.cuerpo(),
            Map.of(Exchange.CONTENT_TYPE, "application/json", "Authorization", "Bearer " + token),
            String.class);
        return convertidor.interpretar(respuesta, peticion);
    }
}