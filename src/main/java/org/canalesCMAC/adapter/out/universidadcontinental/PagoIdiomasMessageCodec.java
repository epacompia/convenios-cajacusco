package org.canalesCMAC.adapter.out.universidadcontinental;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import org.canalesCMAC.application.port.MapeadorOperacion;
import org.canalesCMAC.application.port.PeticionSalida;
import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class PagoIdiomasMessageCodec implements MapeadorOperacion {

    private static final Logger LOG =
        Logger.getLogger(PagoIdiomasMessageCodec.class);

    private static final String CONTENT_TYPE =
        "application/json";

    private final ObjectMapper objectMapper =
        new ObjectMapper();

    @Override
    public Institucion institucion() {
        return Institucion.UNIVCONTINENTAL_CIDIOMAS;
    }

    @Override
    public PeticionSalida construir(
        RecaudacionRequest peticion
    ) {

        LOG.debugf(
            "construyendo request Pago Idiomas operador=%s",
            peticion.operador()
        );

        Map<String, Object> request =
            new LinkedHashMap<>();

        poner(
            request,
            "codigocontrato",
            peticion,
            "codigocontrato"
        );

        poner(
            request,
            "moneda",
            peticion,
            "moneda"
        );

        poner(
            request,
            "canal",
            peticion,
            "canal"
        );

        try {

            String json =
                objectMapper.writeValueAsString(request);

            return new PeticionSalida(
                json,
                CONTENT_TYPE
            );

        } catch (JsonProcessingException e) {

            LOG.error(
                "No se pudo construir request Pago Idiomas",
                e
            );

            throw new IllegalStateException(
                "Error construyendo request Pago Idiomas",
                e
            );
        }
    }

    private void poner(
        Map<String, Object> request,
        String nombre,
        RecaudacionRequest peticion,
        String clave
    ) {

        Object valor =
            peticion.datos().get(clave);

        if (valor != null) {
            request.put(nombre, valor);
        }
    }

    @Override
    public RecaudacionResponse interpretar(
        RespuestaCruda respuesta,
        RecaudacionRequest peticion
    ) {

        String codigo = "";
        String mensaje = "";
        String codigoContrato = "";
        String cliente = "";

        try {

            JsonNode root =
                objectMapper.readTree(
                    respuesta.cuerpo()
                );

            mensaje =
                root.path("Mensaje").asText();

            codigo =
                root.path("Codigo").asText();

            codigoContrato =
                root.path("CodigoContrato").asText();

            cliente =
                root.path("Cliente").asText();

            LOG.debugf(
                "respuesta Pago Idiomas codigo=%s mensaje=%s",
                codigo,
                mensaje
            );

        } catch (JsonProcessingException e) {

            LOG.errorf(
                "respuesta Pago Idiomas inválida: %s",
                e.getMessage()
            );
        }

        Map<String, Object> datos =
            new LinkedHashMap<>();

        datos.put("mensaje", mensaje);
        datos.put("codigoContrato", codigoContrato);
        datos.put("cliente", cliente);

        return new RecaudacionResponse(
            Institucion.UNIVCONTINENTAL_CIDIOMAS,
            peticion.operador(),
            codigo,
            "",
            datos
        );
    }
}
