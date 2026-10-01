package org.canalesCMAC.infrastructure.camel;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.canalesCMAC.application.port.RecaudacionChannel;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RecaudacionCamelChannel implements RecaudacionChannel {

    @Inject
    ProducerTemplate producerTemplate;

    @Override
    public RecaudacionResponse ejecutar(RecaudacionRequest solicitud) {
        try {
            return producerTemplate.requestBody("direct:recaudacion", solicitud, RecaudacionResponse.class);
        } catch (CamelExecutionException excepcion) {
            if (excepcion.getCause() instanceof RuntimeException causa) {
                throw causa;
            }
            throw excepcion;
        }
    }
}
