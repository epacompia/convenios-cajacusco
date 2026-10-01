package org.canalesCMAC.adapter.out.electrosureste;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.canalesCMAC.domain.exception.OperadorNoSoportadoException;
import org.canalesCMAC.domain.model.Canonico;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@ApplicationScoped
public class ElseSoapMessageCodec {

    private static final String SOAP_ENVELOPE_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final DateTimeFormatter FECHA_SOAP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @ConfigProperty(name = "recaudacion.electrosureste.usuario")
    String usuario;

    @ConfigProperty(name = "recaudacion.electrosureste.clave")
    String clave;

    @ConfigProperty(name = "recaudacion.electrosureste.soap.namespace", defaultValue = "http://tempuri.org/")
    String namespace;

    public String serializar(RecaudacionRequest peticion) {
        String operacion = operacion(peticion.operador());
        Map<String, String> campos = campos(peticion.operador(), peticion);
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        xml.append("<soap:Envelope xmlns:soap=\"").append(SOAP_ENVELOPE_NS)
           .append("\" xmlns:els=\"").append(namespace).append("\">");
        xml.append("<soap:Body>");
        xml.append("<els:").append(operacion).append(">");
        campos.forEach((campo, valor) ->
            xml.append("<els:").append(campo).append(">").append(escapar(valor)).append("</els:").append(campo).append(">"));
        xml.append("</els:").append(operacion).append(">");
        xml.append("</soap:Body></soap:Envelope>");
        return xml.toString();
    }

    public RecaudacionResponse deserializar(String xmlSoap, RecaudacionRequest peticion) {
        Element raizRespuesta = raizRespuesta(xmlSoap);
        String codigo = mapeoCodigo(peticion.operador(), raizRespuesta);
        String mensaje = mapeoMensaje(peticion.operador(), raizRespuesta);
        Map<String, Object> datos = "00".equals(codigo)
            ? mapearDatos(peticion.operador(), raizRespuesta)
            : Map.of();
        return new RecaudacionResponse(Institucion.ELSE, peticion.operador(), codigo, mensaje, datos);
    }

    private Map<String, String> campos(Operador operador, RecaudacionRequest peticion) {
        Map<String, String> parametros = new LinkedHashMap<>();
        switch (operador) {
            case CONSULTA_DEUDA -> {
                parametros.put("IdentificadorEntidadConsulta", UUID.randomUUID().toString());
                parametros.put("AgenciaEntidadConsulta", peticion.datos().getOrDefault(Canonico.AGENCIA, "Agencia Principal").toString());
                parametros.put("MedioPagoEntidadConsulta", peticion.datos().getOrDefault(Canonico.MEDIO_PAGO, "Ventanilla").toString());
                parametros.put("FechaConsulta", ahoraSoap());
                parametros.put("UsuarioConsulta", usuario);
                parametros.put("ClaveUsuario", clave);
                parametros.put("TipoConsulta", peticion.datos().getOrDefault(Canonico.TIPO_CONSULTA, "1").toString());
                parametros.put("DatoConsulta", datoObligatorio(peticion, Canonico.DATO_CONSULTA));
            }
            case PAGO_DEUDA, CONSULTA_PAGO -> {
                parametros.put("IdentificadorTransaccion", datoObligatorio(peticion, Canonico.IDENTIFICADOR_TRANSACCION));
                parametros.put("IdentificadorEntidadPago", UUID.randomUUID().toString());
                parametros.put("AgenciaEntidadPago", peticion.datos().getOrDefault(Canonico.AGENCIA, "Agencia Principal").toString());
                parametros.put("MedioPagoEntidadPago", peticion.datos().getOrDefault(Canonico.MEDIO_PAGO, "Ventanilla").toString());
                parametros.put("FechaPago", ahoraSoap());
                parametros.put("UsuarioPago", usuario);
                parametros.put("ClaveUsuario", clave);
                parametros.put("CodigoComprobante", datoObligatorio(peticion, Canonico.NUMERO_COMPROBANTE));
                parametros.put("CodigoSuministro", datoObligatorio(peticion, Canonico.NUMERO_SUMINISTRO));
                parametros.put("MontoPago", monto(peticion));
            }
            case EXTORNO_PAGO -> {
                parametros.put("IdentificadorTransaccion", datoObligatorio(peticion, Canonico.IDENTIFICADOR_TRANSACCION));
                parametros.put("CodigoIdentificadorPago", datoObligatorio(peticion, Canonico.IDENTIFICADOR_PAGO));
                parametros.put("IdentificadorEntidadExtorno", UUID.randomUUID().toString());
                parametros.put("CodigoMotivoExtorno", peticion.datos().getOrDefault(Canonico.MOTIVO_EXSTORNO, "1").toString());
                parametros.put("AgenciaEntidadExtorno", peticion.datos().getOrDefault(Canonico.AGENCIA, "Agencia Principal").toString());
                parametros.put("MedioPagoEntidadExtorno", peticion.datos().getOrDefault(Canonico.MEDIO_PAGO, "Ventanilla").toString());
                parametros.put("FechaExtornoEntidad", ahoraSoap());
                parametros.put("UsuarioExtorno", usuario);
                parametros.put("ClaveUsuario", clave);
                parametros.put("CodigoComprobante", datoObligatorio(peticion, Canonico.NUMERO_COMPROBANTE));
                parametros.put("CodigoSuministro", datoObligatorio(peticion, Canonico.NUMERO_SUMINISTRO));
                parametros.put("MontoExtorno", monto(peticion));
            }
            default -> throw new OperadorNoSoportadoException("Operador " + operador + " no soportado por ELSE");
        }
        return parametros;
    }

    private Map<String, Object> mapearDatos(Operador operador, Element raiz) {
        Map<String, Object> datos = new HashMap<>();
        switch (operador) {
            case CONSULTA_DEUDA -> {
                poner(datos, Canonico.IDENTIFICADOR_TRANSACCION, texto(raiz, "IdentificadorTransaccion"));
                poner(datos, Canonico.NUMERO_COMPROBANTE, texto(raiz, "CodigoComprobante"));
                poner(datos, Canonico.NUMERO_SUMINISTRO, texto(raiz, "CodigoSuministro"));
                poner(datos, Canonico.NOMBRE_CLIENTE, texto(raiz, "NombreCliente"));
                poner(datos, Canonico.DISTRITO_CLIENTE, texto(raiz, "DistritoCliente"));
                poner(datos, Canonico.DIRECCION_CLIENTE, texto(raiz, "DireccionCliente"));
                poner(datos, Canonico.DOCUMENTO_CLIENTE, texto(raiz, "NumeroDocumento"));
                poner(datos, Canonico.MONTO_PAGAR, decimal(texto(raiz, "MontoAPagarConsulta")));
                poner(datos, Canonico.FECHA_EMISION, texto(raiz, "FechaEmision"));
                poner(datos, Canonico.FECHA_VENCIMIENTO, texto(raiz, "FechaVencimiento"));
                poner(datos, Canonico.FECHA_VENCIMIENTO_RSP, texto(raiz, "FechaVencimientoRSP"));
                poner(datos, "detalleConsulta", texto(raiz, "DetalleConsulta"));
            }
            case PAGO_DEUDA -> {
                poner(datos, Canonico.IDENTIFICADOR_TRANSACCION, texto(raiz, "IdentificadorTransaccion"));
                poner(datos, Canonico.IDENTIFICADOR_PAGO, texto(raiz, "CodigoIdentificadorPago"));
                poner(datos, Canonico.NUMERO_COMPROBANTE, texto(raiz, "CodigoComprobante"));
                poner(datos, Canonico.NUMERO_SUMINISTRO, texto(raiz, "CodigoSuministro"));
                poner(datos, Canonico.MONTO, decimal(texto(raiz, "MontoPago")));
                poner(datos, "fechaRegistroPago", texto(raiz, "FechaRegistroPago"));
            }
            case CONSULTA_PAGO -> {
                poner(datos, Canonico.IDENTIFICADOR_TRANSACCION, texto(raiz, "IdentificadorTransaccion"));
                poner(datos, Canonico.IDENTIFICADOR_PAGO, texto(raiz, "CodigoIdentificadorPago"));
                poner(datos, Canonico.NUMERO_COMPROBANTE, texto(raiz, "CodigoComprobante"));
                poner(datos, Canonico.NUMERO_SUMINISTRO, texto(raiz, "CodigoSuministro"));
                poner(datos, Canonico.MONTO, decimal(texto(raiz, "MontoPago")));
                poner(datos, "fechaRegistroPago", texto(raiz, "FechaRegistroPago"));
            }
            case EXTORNO_PAGO -> {
                poner(datos, Canonico.IDENTIFICADOR_TRANSACCION, texto(raiz, "IdentificadorTransaccion"));
                poner(datos, Canonico.IDENTIFICADOR_PAGO, texto(raiz, "CodigoIdentificadorExtorno"));
                poner(datos, Canonico.NUMERO_COMPROBANTE, texto(raiz, "CodigoComprobante"));
                poner(datos, Canonico.NUMERO_SUMINISTRO, texto(raiz, "CodigoSuministro"));
                poner(datos, Canonico.MONTO, decimal(texto(raiz, "MontoExtorno")));
                poner(datos, "fechaRegistroExtorno", texto(raiz, "FechaRegistroExtorno"));
            }
            default -> throw new OperadorNoSoportadoException("Operador " + operador + " no soportado por ELSE");
        }
        return datos;
    }

    private void poner(Map<String, Object> destino, String clave, Object valor) {
        if (valor != null) {
            destino.put(clave, valor);
        }
    }

    private String mapeoCodigo(Operador operador, Element raiz) {
        String campo = switch (operador) {
            case CONSULTA_DEUDA -> "CodigoMensajeRetornoConsulta";
            case PAGO_DEUDA -> "CodigoMensajeRetornoPago";
            case CONSULTA_PAGO -> "CodigoMensajeRetornoConsultaPago";
            case EXTORNO_PAGO -> "CodigoMensajeRetornoExtorno";
            default -> throw new OperadorNoSoportadoException("Operador " + operador + " no soportado por ELSE");
        };
        String codigo = texto(raiz, campo);
        return (codigo == null || codigo.isBlank()) ? "99" : ("0".equals(codigo.trim()) ? "00" : codigo.trim());
    }

    private String mapeoMensaje(Operador operador, Element raiz) {
        String campo = switch (operador) {
            case CONSULTA_DEUDA -> "MensajeRetornoConsulta";
            case PAGO_DEUDA -> "MensajeRetornoPago";
            case CONSULTA_PAGO -> "MensajeRetornoConsultaPago";
            case EXTORNO_PAGO -> "MensajeRetornoExtorno";
            default -> throw new OperadorNoSoportadoException("Operador " + operador + " no soportado por ELSE");
        };
        String mensaje = texto(raiz, campo);
        return mensaje == null ? "" : mensaje.trim();
    }

    private Element raizRespuesta(String xmlSoap) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Document documento = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xmlSoap)));
            Element body = null;
            NodeList hijos = documento.getDocumentElement().getChildNodes();
            for (int i = 0; i < hijos.getLength(); i++) {
                Node n = hijos.item(i);
                if (n instanceof Element e && "Body".equals(nombreLocal(e.getTagName()))) {
                    body = e;
                    break;
                }
            }
            if (body == null) {
                throw new IllegalArgumentException("SOAP sin elemento Body");
            }
            NodeList hijosBody = body.getChildNodes();
            for (int i = 0; i < hijosBody.getLength(); i++) {
                Node n = hijosBody.item(i);
                if (n instanceof Element) {
                    return (Element) n;
                }
            }
            throw new IllegalArgumentException("SOAP sin operación de respuesta");
        } catch (Exception e) {
            throw new IllegalArgumentException("Respuesta SOAP no válida: " + e.getMessage(), e);
        }
    }

    private String texto(Element raiz, String nombre) {
        NodeList hijos = raiz.getChildNodes();
        for (int i = 0; i < hijos.getLength(); i++) {
            Node n = hijos.item(i);
            if (n instanceof Element e && nombre.equals(nombreLocal(e.getTagName()))) {
                return e.getTextContent();
            }
        }
        return null;
    }

    private String nombreLocal(String nombreConPrefijo) {
        int indice = nombreConPrefijo.indexOf(':');
        return indice < 0 ? nombreConPrefijo : nombreConPrefijo.substring(indice + 1);
    }

    private String operacion(Operador operador) {
        return switch (operador) {
            case CONSULTA_DEUDA -> "ConsultaDeuda";
            case PAGO_DEUDA -> "PagoDeuda";
            case CONSULTA_PAGO -> "ConsultaPago";
            case EXTORNO_PAGO -> "ExtornoPago";
            default -> throw new OperadorNoSoportadoException("Operador " + operador + " no soportado por ELSE");
        };
    }

    private String datoObligatorio(RecaudacionRequest peticion, String clave) {
        Object valor = peticion.datos().get(clave);
        if (valor == null || valor.toString().isBlank()) {
            throw new IllegalArgumentException("Campo canónico obligatorio ausente: " + clave);
        }
        return valor.toString();
    }

    private String monto(RecaudacionRequest peticion) {
        Object monto = peticion.datos().get(Canonico.MONTO);
        if (monto == null) {
            throw new IllegalArgumentException("Campo canónico obligatorio ausente: monto");
        }
        return new BigDecimal(monto.toString()).setScale(2).toPlainString();
    }

    private BigDecimal decimal(String valor) {
        return valor == null || valor.isBlank() ? null : new BigDecimal(valor);
    }

    private String ahoraSoap() {
        return LocalDateTime.now().format(FECHA_SOAP);
    }

    private String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}