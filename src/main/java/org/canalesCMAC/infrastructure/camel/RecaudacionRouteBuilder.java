package org.canalesCMAC.infrastructure.camel;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ChoiceDefinition;
import org.canalesCMAC.application.ConvenioRegistro;
import org.canalesCMAC.application.port.ConvenioAdapter;
import org.canalesCMAC.domain.exception.InstitucionNoSoportadaException;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.RecaudacionRequest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class RecaudacionRouteBuilder extends RouteBuilder {

    @Inject
    ConvenioRegistro registro;

    @Inject
    Instance<ConvenioAdapter> adapters;

    @ConfigProperty(name = "recaudacion.reintentos", defaultValue = "0")
    int reintentos;

    @ConfigProperty(name = "recaudacion.reintentos.delay", defaultValue = "500")
    long reintentosDelay;

    @Override
    public void configure() {
        errorHandler(defaultErrorHandler()
            .maximumRedeliveries(reintentos)
            .redeliveryDelay(reintentosDelay)
            .logExhausted(false));

        Map<Institucion, ConvenioAdapter> porInstitucion = new EnumMap<>(Institucion.class);
        for (ConvenioAdapter adaptador : adapters) {
            porInstitucion.put(adaptador.institucion(), adaptador);
        }

        ChoiceDefinition choice = from("direct:recaudacion")
            .routeId("recaudacion.contentBasedRouter")
            .process(exchange -> {
                RecaudacionRequest request = exchange.getMessage().getBody(RecaudacionRequest.class);
                exchange.getMessage().setHeader("institucion", request.institucion().name());
            })
            .choice();

        for (Institucion institucion : registro.instituciones()) {
            if (porInstitucion.containsKey(institucion)) {
                choice.when(header("institucion").isEqualTo(institucion.name()))
                    .to(registro.ruta(institucion));
            }
        }

        choice.otherwise()
            .throwException(new InstitucionNoSoportadaException("Institución no soportada"))
            .end();

        for (Institucion institucion : registro.instituciones()) {
            ConvenioAdapter adaptador = porInstitucion.get(institucion);
            if (adaptador == null) {
                continue;
            }
            from(registro.ruta(institucion))
                .routeId("recaudacion." + institucion.name().toLowerCase(Locale.ROOT) + "." + registro.protocolo(institucion))
                .bean(adaptador, "procesar");
        }
    }
}
