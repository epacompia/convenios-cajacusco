package org.canalesCMAC.domain.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record RecaudacionRequest(Institucion institucion, Operador operador, Map<String, Object> datos) {

    public RecaudacionRequest {
        if (institucion == null || operador == null) {
            throw new IllegalArgumentException("institucion y operador son obligatorios");
        }
        datos = datos == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(datos));
    }
}