package org.canalesCMAC.application;

import org.canalesCMAC.domain.exception.InstitucionNoSoportadaException;
import org.canalesCMAC.domain.exception.OperadorNoSoportadoException;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RecaudacionResolver {

    private final ConvenioRegistro registro;

    @Inject
    public RecaudacionResolver(ConvenioRegistro registro) {
        this.registro = registro;
    }

    public void validar(Institucion institucion, Operador operador) {
        if (!registro.soporta(institucion)) {
            throw new InstitucionNoSoportadaException("Institución no soportada: " + institucion);
        }
        if (!registro.soporta(institucion, operador)) {
            throw new OperadorNoSoportadoException("Operador " + operador + " no soportado para " + institucion);
        }
    }
}
