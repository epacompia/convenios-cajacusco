package org.canalesCMAC.application;

import java.util.Set;

import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;

public record Convenio(
    Institucion institucion,
    String protocolo,
    String url,
    Set<Operador> operadores,
    int reintentos,
    long reintentosDelay,
    long timeout) {
}
