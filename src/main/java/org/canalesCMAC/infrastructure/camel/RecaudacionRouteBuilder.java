package org.canalesCMAC.infrastructure.camel;

import org.apache.camel.builder.RouteBuilder;
import org.canalesCMAC.adapter.out.electrosureste.ElseAdapter;
import org.canalesCMAC.adapter.out.electroucayali.ElectroUcayaliAdapter;
import org.canalesCMAC.adapter.out.seal.SealAdapter;
import org.canalesCMAC.domain.exception.InstitucionNoSoportadaException;
import org.canalesCMAC.domain.model.Institucion;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.canalesCMAC.domain.model.RecaudacionRequest;

@ApplicationScoped
public class RecaudacionRouteBuilder extends RouteBuilder {

    @Inject
    ElseAdapter elseAdapter;

    @Inject
    ElectroUcayaliAdapter electroUcayaliAdapter;

    @Inject
    SealAdapter sealAdapter;

    @Override
    public void configure() {
        from("direct:recaudacion")
            .routeId("recaudacion.contentBasedRouter")
            .process(exchange -> {
                RecaudacionRequest request = exchange.getMessage().getBody(RecaudacionRequest.class);
                exchange.getMessage().setHeader("institucion", request.institucion().name());
            })
            .choice()
                .when(header("institucion").isEqualTo(Institucion.ELSE.name()))
                    .to("direct:else")
                .when(header("institucion").isEqualTo(Institucion.ELECTRO_UCAYALI.name()))
                    .to("direct:electroucayali")
                .when(header("institucion").isEqualTo(Institucion.SEAL.name()))
                    .to("direct:seal")
                .otherwise()
                    .throwException(new InstitucionNoSoportadaException("Institución no soportada"))
            .end();

        from("direct:else")
            .routeId("recaudacion.electrosureste.soap")
            .bean(elseAdapter, "procesar");

        from("direct:electroucayali")
            .routeId("recaudacion.electroucayali.rest")
            .bean(electroUcayaliAdapter, "procesar");

        from("direct:seal")
            .routeId("recaudacion.seal.iso8583")
            .bean(sealAdapter, "procesar");
    }
}