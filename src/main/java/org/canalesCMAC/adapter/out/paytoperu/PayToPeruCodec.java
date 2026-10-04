package org.canalesCMAC.adapter.out.paytoperu;

import java.util.LinkedHashMap;
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
public class PayToPeruCodec implements MapeadorOperacion {

private static final Logger LOG = Logger.getLogger(PayToPeruCodec.class);
private static final String CONTENT_TYPE = "application/json";

private final ObjectMapper objectMapper = new ObjectMapper();

@Override
public Institucion institucion() {
    return Institucion.PAYTOPERU;
}

@Override
public PeticionSalida construir(RecaudacionRequest peticion) {
    LOG.debugf("construyendo REST PAYTOPERU operador=%s", peticion.operador());

    Map<String, Object> request = new LinkedHashMap<>();

    request.put(
        "ncodigo_pago",
        peticion.datos().get("ncodigo_pago")
    );

    request.put(
        "cnro_documento",
        peticion.datos().get("cnro_documento")
    );

    try {
        return new PeticionSalida(objectMapper.writeValueAsString(request), CONTENT_TYPE);
    } catch (JsonProcessingException e) {
        throw new IllegalStateException(
            "Error serializando request de PayToPeru",
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

        String codigoRespuesta =
            root.path("berror").asBoolean()
                ? "01"
                : "00";

        String mensaje =
            root.path("cmensaje").asText();

        Map<String, Object> datos =
            new LinkedHashMap<>();

        datos.put(
            "ncodigo_pago",
            root.path("ncodigo_pago").asLong()
        );

        datos.put(
            "berror",
            root.path("berror").asBoolean()
        );

        datos.put(
            "cmensaje",
            root.path("cmensaje").asText()
        );

        datos.put(
            "cnro_documento",
            root.path("cnro_documento").asText()
        );

        datos.put(
            "cnombres",
            root.path("cnombres").asText()
        );

        datos.put(
            "capellidos",
            root.path("capellidos").asText()
        );

        datos.put(
            "cemail",
            root.path("cemail").asText()
        );

        datos.put(
            "nimporte",
            root.path("nimporte").asDouble()
        );

        datos.put(
            "nmoneda",
            root.path("nmoneda").asInt()
        );

        datos.put(
            "cconcepto",
            root.path("cconcepto").asText()
        );

        return new RecaudacionResponse(
            Institucion.PAYTOPERU,
            peticion.operador(),
            codigoRespuesta,
            mensaje,
            datos
        );

    } catch (JsonProcessingException e) {
        throw new IllegalStateException(
            "Error deserializando respuesta de PayToPeru",
            e
        );
    }
}

}