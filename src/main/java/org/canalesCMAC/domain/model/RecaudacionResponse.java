package org.canalesCMAC.domain.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record RecaudacionResponse(Institucion institucion, Operador operador, String codigo, String mensaje, Map<String, Object> datos) {

    public RecaudacionResponse {
        datos = datos == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(datos));
    }
}