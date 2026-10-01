package org.canalesCMAC.application;

import org.canalesCMAC.application.port.RecaudacionChannel;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RecaudacionUseCaseImpl implements RecaudacionUseCase {

    @Inject
    RecaudacionResolver resolver;

    @Inject
    RecaudacionChannel canal;

    @Override
    public RecaudacionResponse procesar(RecaudacionRequest solicitud) {
        resolver.validar(solicitud.institucion(), solicitud.operador());
        return canal.ejecutar(solicitud);
    }
}