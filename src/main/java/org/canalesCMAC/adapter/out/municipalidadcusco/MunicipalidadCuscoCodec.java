package org.canalesCMAC.adapter.out.municipalidadcusco;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;

import org.canalesCMAC.application.port.MapeadorOperacion;
import org.canalesCMAC.application.port.PeticionSalida;
import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MunicipalidadCuscoCodec implements MapeadorOperacion {

private static final Logger LOG = Logger.getLogger(MunicipalidadCuscoCodec.class);
private static final String CONTENT_TYPE = "application/json";

private final ObjectMapper objectMapper = new ObjectMapper();

@Override
public Institucion institucion() {
    return Institucion.MUNICIPALIDAD_CUSCO;
}

@Override
public PeticionSalida construir(RecaudacionRequest peticion) {
    LOG.debugf("construyendo REST MUNICIPALIDAD_CUSCO operador=%s", peticion.operador());

    Map<String, Object> request = Map.of(
        "ordenpago",
        peticion.datos().get("ordenpago")
    );

    try {
        return new PeticionSalida(objectMapper.writeValueAsString(request), CONTENT_TYPE);
    } catch (JsonProcessingException e) {
        throw new IllegalStateException(
            "Error serializando request de recuperar orden de pago",
            e
        );
    }
}

@Override
public RecaudacionResponse interpretar(
    RespuestaCruda respuesta,
    RecaudacionRequest peticion) {

    try {
        JsonNode root = objectMapper.readTree(respuesta.cuerpo());

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