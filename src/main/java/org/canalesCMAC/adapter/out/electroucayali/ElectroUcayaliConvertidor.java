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

import org.canalesCMAC.application.CatalogoCodigos;
import org.canalesCMAC.application.port.MapeadorOperacion;
import org.canalesCMAC.application.port.PeticionSalida;
import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.exception.OperadorNoSoportadoException;
import org.canalesCMAC.domain.model.Canonico;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ElectroUcayaliConvertidor implements MapeadorOperacion {

    private static final Logger LOG = Logger.getLogger(ElectroUcayaliConvertidor.class);
    private static final String CONTENT_TYPE = "application/json";
    private static final DateTimeFormatter FECHA_COMPACTA = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HORA_COMPACTA = DateTimeFormatter.ofPattern("HHmmss");

    @Inject
    ObjectMapper objectMapper;

    @Inject
    CatalogoCodigos catalogo;

    @ConfigProperty(name = "recaudacion.electroucayali.codempresa", defaultValue = "05")
    String codEmpresa;

    @Override
    public Institucion institucion() {
        return Institucion.ELECTRO_UCAYALI;
    }

    @Override
    public PeticionSalida construir(RecaudacionRequest peticion) {
        OperacionEu operacion = OperacionEu.de(peticion.operador());
        LOG.debugf("construyendo REST Electro Ucayali operador=%s ruta=%s", peticion.operador(), operacion.ruta());
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        operacion.cuerpo(cuerpo, peticion, codEmpresa);
        return new PeticionSalida(jsonDe(cuerpo), operacion.ruta(), CONTENT_TYPE);
    }

    @Override
    public RecaudacionResponse interpretar(RespuestaCruda respuesta, RecaudacionRequest peticion) {
        try {
            JsonNode nodo = objectMapper.readTree(respuesta.cuerpo());
            String codigo = nodo.path("codigo").asText("99");
            String mensaje = catalogo.mensaje(Institucion.ELECTRO_UCAYALI, codigo, nodo.path("mensaje").asText(""));
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
            LOG.debugf("REST Electro Ucayali interpretado operador=%s codigo=%s", peticion.operador(), codigo);
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

    private static LocalDateTime fechaOperacion(RecaudacionRequest peticion) {
        Object valor = peticion.datos().get(Canonico.FECHA_OPERACION);
        if (valor != null && !valor.toString().isBlank()) {
            return LocalDateTime.parse(valor.toString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        return LocalDateTime.now();
    }

    private static String dato(RecaudacionRequest peticion, String clave) {
        Object valor = peticion.datos().get(clave);
        if (valor == null || valor.toString().isBlank()) {
            throw new IllegalArgumentException("Campo canónico obligatorio ausente: " + clave);
        }
        return valor.toString();
    }

    private static BigDecimal monto(RecaudacionRequest peticion) {
        Object monto = peticion.datos().get(Canonico.MONTO);
        if (monto == null) {
            throw new IllegalArgumentException("Campo canónico obligatorio ausente: monto");
        }
        return new BigDecimal(monto.toString()).setScale(2);
    }

    private static void extraerSiExiste(JsonNode nodo, Map<String, Object> destino, String origen, String destinoClave) {
        if (nodo.hasNonNull(origen) && !nodo.get(origen).asText().isBlank()) {
            destino.put(destinoClave, nodo.get(origen).asText());
        }
    }

    private enum OperacionEu {
        CONSULTA_DEUDA {
            @Override
            String ruta() {
                return "/api/v2/electro/consulta";
            }

            @Override
            void cuerpo(Map<String, Object> c, RecaudacionRequest p, String codEmpresa) {
                c.put("nroSumin", dato(p, Canonico.NUMERO_SUMINISTRO));
                c.put("traceConsulta", p.datos().getOrDefault(Canonico.TRACE_CONSULTA, "").toString());
                c.put("fechaConsulta", FECHA_COMPACTA.format(fechaOperacion(p)));
                c.put("horaConsulta", HORA_COMPACTA.format(fechaOperacion(p)));
                comunes(c, codEmpresa);
            }
        },
        PAGO_DEUDA {
            @Override
            String ruta() {
                return "/api/v1/electro/pago";
            }

            @Override
            void cuerpo(Map<String, Object> c, RecaudacionRequest p, String codEmpresa) {
                c.put("nroSumin", dato(p, Canonico.NUMERO_SUMINISTRO));
                c.put("numFactura", dato(p, Canonico.NUMERO_COMPROBANTE));
                c.put("traceConsulta", p.datos().getOrDefault(Canonico.TRACE_CONSULTA, "").toString());
                c.put("fechaPago", FECHA_COMPACTA.format(fechaOperacion(p)));
                c.put("horaPago", HORA_COMPACTA.format(fechaOperacion(p)));
                c.put("montoDeuda", monto(p));
                comunes(c, codEmpresa);
            }
        },
        EXTORNO_PAGO {
            @Override
            String ruta() {
                return "/api/v1/electro/extorno";
            }

            @Override
            void cuerpo(Map<String, Object> c, RecaudacionRequest p, String codEmpresa) {
                c.put("nroSumin", dato(p, Canonico.NUMERO_SUMINISTRO));
                c.put("numFactura", dato(p, Canonico.NUMERO_COMPROBANTE));
                c.put("traceConsulta", p.datos().getOrDefault(Canonico.TRACE_CONSULTA, "").toString());
                c.put("fechaPago", FECHA_COMPACTA.format(fechaOperacion(p)));
                c.put("horaPago", HORA_COMPACTA.format(fechaOperacion(p)));
                c.put("montoDeuda", monto(p));
                comunes(c, codEmpresa);
            }
        },
        EXTORNO_PAGO_AUTO {
            @Override
            String ruta() {
                return "/api/v1/electro/extorno_automatico_pago";
            }

            @Override
            void cuerpo(Map<String, Object> c, RecaudacionRequest p, String codEmpresa) {
                c.put("nroSumin", dato(p, Canonico.NUMERO_SUMINISTRO));
                c.put("numFactura", dato(p, Canonico.NUMERO_COMPROBANTE));
                c.put("fechaPago", FECHA_COMPACTA.format(fechaOperacion(p)));
                c.put("horaPago", HORA_COMPACTA.format(fechaOperacion(p)));
                c.put("montoDeuda", monto(p));
                comunes(c, codEmpresa);
            }
        },
        EXTORNO_AUTO {
            @Override
            String ruta() {
                return "/api/v1/electro/extorno_automatico";
            }

            @Override
            void cuerpo(Map<String, Object> c, RecaudacionRequest p, String codEmpresa) {
                c.put("nroSumin", dato(p, Canonico.NUMERO_SUMINISTRO));
                c.put("numFactura", dato(p, Canonico.NUMERO_COMPROBANTE));
                c.put("tracePago", p.datos().getOrDefault(Canonico.IDENTIFICADOR_PAGO, "").toString());
                c.put("fechaPago", FECHA_COMPACTA.format(fechaOperacion(p)));
                c.put("horaPago", HORA_COMPACTA.format(fechaOperacion(p)));
                c.put("montoDeuda", monto(p));
                comunes(c, codEmpresa);
            }
        };

        abstract String ruta();

        abstract void cuerpo(Map<String, Object> cuerpo, RecaudacionRequest peticion, String codEmpresa);

        static OperacionEu de(Operador operador) {
            for (OperacionEu operacion : values()) {
                if (operacion.name().equals(operador.name())) {
                    return operacion;
                }
            }
            throw new OperadorNoSoportadoException("Operador " + operador + " no soportado por Electro Ucayali");
        }

        private static void comunes(Map<String, Object> c, String codEmpresa) {
            c.put("codEmpresa", codEmpresa);
            c.put("codServicio", "0");
            c.put("codAgencia", "0");
            c.put("codCanal", "00");
            c.put("terminal", "0");
        }
    }
}
