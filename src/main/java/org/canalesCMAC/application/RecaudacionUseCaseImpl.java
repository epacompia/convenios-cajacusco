package org.canalesCMAC.application;

import org.canalesCMAC.application.port.RecaudacionChannel;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RecaudacionUseCaseImpl implements RecaudacionUseCase {

    private static final Logger LOG = Logger.getLogger(RecaudacionUseCaseImpl.class);

    @Inject
    RecaudacionResolver resolver;

    @Inject
    RecaudacionChannel canal;

    @Override
    public RecaudacionResponse procesar(RecaudacionRequest solicitud) {
        resolver.validar(solicitud.institucion(), solicitud.operador());
        RecaudacionResponse respuesta = canal.ejecutar(solicitud);
        LOG.infof("recaudacion institucion=%s operador=%s codigo=%s",
            solicitud.institucion(), solicitud.operador(), respuesta.codigo());
        return respuesta;
    }
}
