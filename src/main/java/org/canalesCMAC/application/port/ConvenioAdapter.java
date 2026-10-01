package org.canalesCMAC.application.port;

import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;

public interface ConvenioAdapter {

    Institucion institucion();

    RecaudacionResponse procesar(RecaudacionRequest peticion);
}
