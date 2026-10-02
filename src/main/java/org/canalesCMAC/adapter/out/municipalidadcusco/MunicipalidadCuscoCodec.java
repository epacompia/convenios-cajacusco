package org.canalesCMAC.adapter.out.municipalidadcusco;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;

import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;

@ApplicationScoped
public class MunicipalidadCuscoCodec {

private final ObjectMapper objectMapper = new ObjectMapper();

public String serializar(RecaudacionRequest peticion) {

    Map<String, Object> request = Map.of(
        "ordenpago",
        peticion.datos().get("ordenpago")
    );

    try {
        return objectMapper.writeValueAsString(request);
    } catch (JsonProcessingException e) {
        throw new IllegalStateException(
            "Error serializando request de recuperar orden de pago",
            e
        );
    }
}

public RecaudacionResponse deserializar(
    String json,
    RecaudacionRequest peticion) {

    try {
        JsonNode root = objectMapper.readTree(json);

        String status = root.path("status").asText();
        String message = root.path("message").asText();

        JsonNode data = root.path("data");

        Map<String, Object> datos = Map.of(
            "status", status,
            "message", message,
            "data", convertirData(data)
        );

        return new RecaudacionResponse(
            Institucion.MUNICIPALIDAD_CUSCO,
            peticion.operador(),
            status,
            message,
            datos
        );

    } catch (JsonProcessingException e) {
        throw new IllegalStateException(
            "Error deserializando respuesta de recuperar orden de pago",
            e
        );
    }
}

private List<Map<String, Object>> convertirData(JsonNode data) {

    if (!data.isArray()) {
        return List.of();
    }

    return objectMapper.convertValue(
        data,
        objectMapper.getTypeFactory()
            .constructCollectionType(
                List.class,
                Map.class
            )
    );
}

}