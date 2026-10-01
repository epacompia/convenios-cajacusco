package org.canalesCMAC.adapter.out.electroucayali;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.canalesCMAC.domain.exception.OperadorNoSoportadoException;
import org.canalesCMAC.domain.model.Canonico;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ElectroUcayaliConvertidor {

    private static final DateTimeFormatter FECHA_COMPACTA = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HORA_COMPACTA = DateTimeFormatter.ofPattern("HHmmss");

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "recaudacion.electroucayali.codempresa", defaultValue = "05")
    String codEmpresa;

    public record Solicitud(String ruta, String cuerpo) {
    }

    public Solicitud construir(RecaudacionRequest peticion) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        String ruta;
        switch (peticion.operador()) {
            case CONSULTA_DEUDA -> {
                ruta = "/api/v2/electro/consulta";
                cuerpo.put("nroSumin", dato(peticion, Canonico.NUMERO_SUMINISTRO));
                cuerpo.put("traceConsulta", peticion.datos().getOrDefault(Canonico.TRACE_CONSULTA, "").toString());
                cuerpo.put("fechaConsulta", FECHA_COMPACTA.format(fechaOperacion(peticion)));
                cuerpo.put("horaConsulta", HORA_COMPACTA.format(fechaOperacion(peticion)));
                cuerpo.put("codEmpresa", codEmpresa);
                cuerpo.put("codServicio", "0");
                cuerpo.put("codAgencia", "0");
                cuerpo.put("codCanal", "00");
                cuerpo.put("terminal", "0");
            }
            case PAGO_DEUDA -> {
                ruta = "/api/v1/electro/pago";
                cuerpo.put("nroSumin", dato(peticion, Canonico.NUMERO_SUMINISTRO));
                cuerpo.put("numFactura", dato(peticion, Canonico.NUMERO_COMPROBANTE));
                cuerpo.put("traceConsulta", peticion.datos().getOrDefault(Canonico.TRACE_CONSULTA, "").toString());
                cuerpo.put("fechaPago", FECHA_COMPACTA.format(fechaOperacion(peticion)));
                cuerpo.put("horaPago", HORA_COMPACTA.format(fechaOperacion(peticion)));
                cuerpo.put("montoDeuda", monto(peticion));
                cuerpo.put("codEmpresa", codEmpresa);
                cuerpo.put("codServicio", "0");
                cuerpo.put("codAgencia", "0");
                cuerpo.put("codCanal", "00");
                cuerpo.put("terminal", "0");
            }
            case EXTORNO_PAGO -> {
                ruta = "/api/v1/electro/extorno";
                cuerpo.put("nroSumin", dato(peticion, Canonico.NUMERO_SUMINISTRO));
                cuerpo.put("numFactura", dato(peticion, Canonico.NUMERO_COMPROBANTE));
                cuerpo.put("traceConsulta", peticion.datos().getOrDefault(Canonico.TRACE_CONSULTA, "").toString());
                cuerpo.put("fechaPago", FECHA_COMPACTA.format(fechaOperacion(peticion)));
                cuerpo.put("horaPago", HORA_COMPACTA.format(fechaOperacion(peticion)));
                cuerpo.put("montoDeuda", monto(peticion));
                cuerpo.put("codEmpresa", codEmpresa);
                cuerpo.put("codServicio", "0");
                cuerpo.put("codAgencia", "0");
                cuerpo.put("codCanal", "00");
                cuerpo.put("terminal", "0");
            }
            case EXTORNO_PAGO_AUTO -> {
                ruta = "/api/v1/electro/extorno_automatico_pago";
                cuerpo.put("nroSumin", dato(peticion, Canonico.NUMERO_SUMINISTRO));
                cuerpo.put("numFactura", dato(peticion, Canonico.NUMERO_COMPROBANTE));
                cuerpo.put("fechaPago", FECHA_COMPACTA.format(fechaOperacion(peticion)));
                cuerpo.put("horaPago", HORA_COMPACTA.format(fechaOperacion(peticion)));
                cuerpo.put("montoDeuda", monto(peticion));
                cuerpo.put("codEmpresa", codEmpresa);
                cuerpo.put("codServicio", "0");
                cuerpo.put("codAgencia", "0");
                cuerpo.put("codCanal", "00");
                cuerpo.put("terminal", "0");
            }
            case EXTORNO_AUTO -> {
                ruta = "/api/v1/electro/extorno_automatico";
                cuerpo.put("nroSumin", dato(peticion, Canonico.NUMERO_SUMINISTRO));
                cuerpo.put("numFactura", dato(peticion, Canonico.NUMERO_COMPROBANTE));
                cuerpo.put("tracePago", peticion.datos().getOrDefault(Canonico.IDENTIFICADOR_PAGO, "").toString());
                cuerpo.put("fechaPago", FECHA_COMPACTA.format(fechaOperacion(peticion)));
                cuerpo.put("horaPago", HORA_COMPACTA.format(fechaOperacion(peticion)));
                cuerpo.put("montoDeuda", monto(peticion));
                cuerpo.put("codEmpresa", codEmpresa);
                cuerpo.put("codServicio", "0");
                cuerpo.put("codAgencia", "0");
                cuerpo.put("codCanal", "00");
                cuerpo.put("terminal", "0");
            }
            default -> throw new OperadorNoSoportadoException("Operador " + peticion.operador() + " no soportado por Electro Ucayali");
        }
        return new Solicitud(ruta, jsonDe(cuerpo));
    }

    public RecaudacionResponse interpretar(String json, RecaudacionRequest peticion) {
        try {
            JsonNode nodo = objectMapper.readTree(json);
            String codigo = nodo.path("codigo").asText("99");
            String mensaje = nodo.path("mensaje").asText("");
            Map<String, Object> datos = new LinkedHashMap<>();
            extraerSiExiste(nodo, datos, "nroSumin", Canonico.NUMERO_SUMINISTRO);
            extraerSiExiste(nodo, datos, "nombreCliente", Canonico.NOMBRE_CLIENTE);
            extraerSiExiste(nodo, datos, "numOperacionEmpresa", Canonico.NUM_OPERACION);
            if (nodo.hasNonNull("lstdebt")) {
                List<Map<String, Object>> deudas = new ArrayList<>();
                for (JsonNode deuda : nodo.get("lstdebt")) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    extraerSiExiste(deuda, item, "numFactura", Canonico.NUMERO_COMPROBANTE);
                    if (deuda.hasNonNull("montoDeuda")) {
                        item.put(Canonico.MONTO, deuda.get("montoDeuda").decimalValue());
                    }
                    extraerSiExiste(deuda, item, "fechaEmision", Canonico.FECHA_EMISION);
                    extraerSiExiste(deuda, item, "fechaVencimiento", Canonico.FECHA_VENCIMIENTO);
                    if (deuda.hasNonNull("glosa")) {
                        item.put("glosa", deuda.get("glosa").asText());
                    }
                    deudas.add(item);
                }
                datos.put(Canonico.LISTA_DEUDAS, deudas);
            }
            return new RecaudacionResponse(Institucion.ELECTRO_UCAYALI, peticion.operador(), codigo, mensaje, datos);
        } catch (Exception e) {
            throw new IllegalArgumentException("Respuesta JSON no válida: " + e.getMessage(), e);
        }
    }

    private String jsonDe(Map<String, Object> cuerpo) {
        try {
            return objectMapper.writeValueAsString(cuerpo);
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo serializar la solicitud JSON", e);
        }
    }

    private LocalDateTime fechaOperacion(RecaudacionRequest peticion) {
        Object valor = peticion.datos().get(Canonico.FECHA_OPERACION);
        if (valor != null && !valor.toString().isBlank()) {
            return LocalDateTime.parse(valor.toString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        return LocalDateTime.now();
    }

    private String dato(RecaudacionRequest peticion, String clave) {
        Object valor = peticion.datos().get(clave);
        if (valor == null || valor.toString().isBlank()) {
            throw new IllegalArgumentException("Campo canónico obligatorio ausente: " + clave);
        }
        return valor.toString();
    }

    private BigDecimal monto(RecaudacionRequest peticion) {
        Object monto = peticion.datos().get(Canonico.MONTO);
        if (monto == null) {
            throw new IllegalArgumentException("Campo canónico obligatorio ausente: monto");
        }
        return new BigDecimal(monto.toString()).setScale(2);
    }

    private void extraerSiExiste(JsonNode nodo, Map<String, Object> destino, String origen, String destinoClave) {
        if (nodo.hasNonNull(origen) && !nodo.get(origen).asText().isBlank()) {
            destino.put(destinoClave, nodo.get(origen).asText());
        }
    }
}