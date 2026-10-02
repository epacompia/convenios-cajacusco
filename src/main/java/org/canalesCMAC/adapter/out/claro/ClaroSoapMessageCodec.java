package org.canalesCMAC.adapter.out.claro;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import org.canalesCMAC.domain.model.Canonico;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

@ApplicationScoped
public class ClaroSoapMessageCodec {

    private static final String SOAP_NS =
        "http://schemas.xmlsoap.org/soap/envelope/";

    private static final String SERVICE_NS =
        "http://servicio.interfazST.webservice.novatronic.com";

    private static final String XSD_NS =
        "http://entidades.interfazST.webservice.novatronic.com/xsd";

    public String serializar(RecaudacionRequest peticion) {

        Map<String, String> campos = campos(peticion);
        StringBuilder xml = new StringBuilder();

        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<soapenv:Envelope")
            .append(" xmlns:soapenv=\"").append(SOAP_NS).append("\"")
            .append(" xmlns:ser=\"").append(SERVICE_NS).append("\"")
            .append(" xmlns:xsd=\"").append(XSD_NS).append("\">");

        xml.append("<soapenv:Header/>");
        xml.append("<soapenv:Body>");
        xml.append("<ser:pago>");
        xml.append("<ser:pagoRequest>");

        campos.forEach((campo, valor) -> {
                if (valor != null) {
                    xml.append("<xsd:").append(campo).append(">");
                    xml.append(escapar(valor));
                    xml.append("</xsd:").append(campo).append(">");
                }
            }
        );

        xml.append("</ser:pagoRequest>");
        xml.append("</ser:pago>");
        xml.append("</soapenv:Body>");
        xml.append("</soapenv:Envelope>");

        return xml.toString();
    }

    private Map<String, String> campos(RecaudacionRequest peticion) {

        Map<String, String> campos = new LinkedHashMap<>();

        poner(campos, "binAdquiriente", peticion, "binAdquiriente");
        poner(campos, "binAdquirienteReenvia", peticion, "binAdquirienteReenvia");
        poner(campos, "canal", peticion, "canal");
        poner(campos, "codigoMoneda", peticion, "codigoMoneda");
        poner(campos, "fechaCaptura", peticion, "fechaCaptura");
        poner(campos, "fechaTransaccion", peticion, "fechaTransaccion");
        poner(campos, "mac", peticion, "mac");
        poner(campos, "nombreComercio", peticion, "nombreComercio");
        poner(campos, "numeroComercio", peticion, "numeroComercio");
        poner(campos, "numeroReferencia", peticion, "numeroReferencia");
        poner(campos, "numeroTerminal", peticion, "numeroTerminal");
        poner(campos, "trace", peticion, "trace");
        poner(campos, "cuentaDestino", peticion, "cuentaDestino");
        poner(campos, "cuentaOrigen", peticion, "cuentaOrigen");
        poner(campos, "fechaExpiracionTarjeta", peticion, "fechaExpiracionTarjeta");
        poner(campos, "numeroTarjeta", peticion, "numeroTarjeta");
        poner(campos, "pinblock", peticion, "pinblock");
        poner(campos, "track2Data", peticion, "track2Data");
        poner(campos, "acreedor", peticion, "acreedor");
        poner(campos, "agencia", peticion, "agencia");
        poner(campos, "ciudad", peticion, "ciudad");
        poner(campos, "codigoFormato", peticion, "codigoFormato");
        poner(campos, "codigoMedioPago", peticion, "codigoMedioPago");
        poner(campos, "codigoMonedaPago", peticion, "codigoMonedaPago");
        poner(campos, "datosTransaccion", peticion, "datosTransaccion");
        poner(campos, "importePago", peticion, Canonico.MONTO);
        poner(campos, "numeroIdentificacionDeudor",
            peticion, "numeroIdentificacionDeudor");
        poner(campos, "numeroProductosPagados",
            peticion, "numeroProductosPagados");
        poner(campos, "pagoTotal", peticion, "pagoTotal");
        poner(campos, "plaza", peticion, "plaza");
        poner(campos, "procesador", peticion, "procesador");
        poner(campos, "tipoCambio", peticion, "tipoCambio");
        poner(campos, "tipoIdentificacionDeudor",
            peticion, "tipoIdentificacionDeudor");

        return campos;
    }

    private void poner(
        Map<String, String> campos,
        String nombre,
        RecaudacionRequest peticion,
        String clave
    ) {
        Object valor = peticion.datos().get(clave);

        if (valor != null && !valor.toString().isBlank()) {
            campos.put(nombre, valor.toString());
        }
    }

    private String escapar(String valor) {

        return valor
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    public RecaudacionResponse deserializar(
    String xmlSoap,
    RecaudacionRequest peticion) {
        //System.out.println("Respuesta SOAP:");
        //System.out.println(xmlSoap);
        XmlMapper xmlMapper = new XmlMapper();
        String codigoRespuesta = "";
        String codigoAutorizacion = "";
        try {
            JsonNode root = xmlMapper.readTree(xmlSoap);
            JsonNode returnNode = root
            .path("Body")
            .path("pagoResponse")
            .path("return");
            codigoRespuesta =
                returnNode.path("codigoRespuesta").asText();
            codigoAutorizacion =
                returnNode.path("codigoAutorizacion").asText();
            System.out.println("====>" + codigoRespuesta);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return new RecaudacionResponse(
            Institucion.CLARO,
            peticion.operador(),
            codigoRespuesta,
            codigoAutorizacion,
            Map.of()
        );
    }
}
