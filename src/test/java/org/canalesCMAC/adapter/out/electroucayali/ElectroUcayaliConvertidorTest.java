package org.canalesCMAC.adapter.out.electroucayali;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ElectroUcayaliConvertidorTest {

    @Inject
    ElectroUcayaliConvertidor convertidor;

    @Test
    void construyeConsultaComoJson() {
        RecaudacionRequest peticion = new RecaudacionRequest(Institucion.ELECTRO_UCAYALI, Operador.CONSULTA_DEUDA,
            Map.of("numeroSuministro", "261150", "fechaOperacion", "2020-03-18T16:25:00", "traceConsulta", "abc"));
        ElectroUcayaliConvertidor.Solicitud solicitud = convertidor.construir(peticion);
        assertEquals("/api/v2/electro/consulta", solicitud.ruta());
        assertTrue(solicitud.cuerpo().contains("\"nroSumin\":\"261150\""));
        assertTrue(solicitud.cuerpo().contains("\"fechaConsulta\":\"20200318\""));
        assertTrue(solicitud.cuerpo().contains("\"horaConsulta\":\"162500\""));
        assertTrue(solicitud.cuerpo().contains("\"codEmpresa\":\"05\""));
    }

    @Test
    void interpretaConsultaConDeudas() {
        String json = "{\"codigo\":\"00\",\"mensaje\":\"Exitoso\",\"nombreCliente\":\"JAMEN AGRO FOREST S.A.\","
            + "\"nroSumin\":\"261150\",\"lstdebt\":[{\"numFactura\":\"201800100000\",\"montoDeuda\":19422.80,"
            + "\"fechaEmision\":\"20181122\",\"fechaVencimiento\":\"20181122\",\"tipoIntegracion\":\"Online\",\"glosa\":\"\"}]}";
        RecaudacionResponse respuesta = convertidor.interpretar(json,
            new RecaudacionRequest(Institucion.ELECTRO_UCAYALI, Operador.CONSULTA_DEUDA, Map.of()));
        assertEquals("00", respuesta.codigo());
        assertEquals("JAMEN AGRO FOREST S.A.", respuesta.datos().get("nombreCliente"));
        List<?> deudas = (List<?>) respuesta.datos().get("listaDeudas");
        assertEquals(1, deudas.size());
        Map<?, ?> primera = (Map<?, ?>) deudas.get(0);
        assertEquals("201800100000", primera.get("numeroComprobante"));
        assertEquals(0, new BigDecimal("19422.80").compareTo((BigDecimal) primera.get("monto")));
    }
}