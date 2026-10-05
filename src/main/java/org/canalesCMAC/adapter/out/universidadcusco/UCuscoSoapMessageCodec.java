package org.canalesCMAC.adapter.out.universidadcusco;

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

@ApplicationScoped
public class UCuscoSoapMessageCodec implements MapeadorOperacion {

    private static final Logger LOG = Logger.getLogger(UCuscoSoapMessageCodec.class);
    private static final String CONTENT_TYPE = "text/xml; charset=utf-8";

    private static final String SOAP_NS =
        "http://schemas.xmlsoap.org/soap/envelope/";

    private static final String UAC_NS =
        "http://www.uac.com.pe";

    @Override
    public Institucion institucion() {
        return Institucion.UNIVCUSCO;
    }

    @Override
    public PeticionSalida construir(RecaudacionRequest peticion) {
        LOG.debugf("construyendo SOAP UNIVCUSCO operador=%s", peticion.operador());
        Map<String, String> campos = campos(peticion);

        StringBuilder xml = new StringBuilder();

        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

        xml.append("<soapenv:Envelope")
            .append(" xmlns:soapenv=\"").append(SOAP_NS).append("\"")
            .append(" xmlns:uac=\"").append(UAC_NS).append("\">");

        xml.append("<soapenv:Header/>");

        xml.append("<soapenv:Body>");

        xml.append("<uac:AutoExtorno>");

        xml.append("<uac:recaudosRq>");

        // cabecera
        xml.append("<uac:cabecera>");

        xml.append("<uac:operacion>");

        agregar(xml, "codigoOperacion", campos.get("codigoOperacion"));
        agregar(xml, "numeroOperacion", campos.get("numeroOperacion"));
        agregar(xml, "codigoBanco", campos.get("codigoBanco"));
        agregar(xml, "codigoConvenio", campos.get("codigoConvenio"));
        agregar(xml, "canalOperacion", campos.get("canalOperacion"));
        agregar(xml, "codigoOficina", campos.get("codigoOficina"));
        agregar(xml, "fechaOperacion", campos.get("fechaOperacion"));
        agregar(xml, "horaOperacion", campos.get("horaOperacion"));

        xml.append("</uac:operacion>");

        xml.append("</uac:cabecera>");

        // detalle
        xml.append("<uac:detalle>");

        xml.append("<uac:transaccion>");

        agregar(
            xml,
            "numeroReferencialDeuda",
            campos.get("numeroReferencialDeuda")
        );

        agregar(
            xml,
            "numeroOperacionRecaudacion",
            campos.get("numeroOperacionRecaudacion")
        );

        xml.append("</uac:transaccion>");

        xml.append("</uac:detalle>");

        xml.append("</uac:recaudosRq>");

        xml.append("</uac:AutoExtorno>");

        xml.append("</soapenv:Body>");

        xml.append("</soapenv:Envelope>");

        return new PeticionSalida(xml.toString(), CONTENT_TYPE);
    }

    private Map<String, String> campos(
        RecaudacionRequest peticion
    ) {

        Map<String, String> campos = new LinkedHashMap<>();

        poner(campos, "codigoOperacion", peticion, "codigoOperacion");
        poner(campos, "numeroOperacion", peticion, "numeroOperacion");
        poner(campos, "codigoBanco", peticion, "codigoBanco");
        poner(campos, "codigoConvenio", peticion, "codigoConvenio");
        poner(campos, "canalOperacion", peticion, "canalOperacion");
        poner(campos, "codigoOficina", peticion, "codigoOficina");
        poner(campos, "fechaOperacion", peticion, "fechaOperacion");
        poner(campos, "horaOperacion", peticion, "horaOperacion");

        poner(
            campos,
            "numeroReferencialDeuda",
            peticion,
            "numeroReferencialDeuda"
        );

        poner(
            campos,
            "numeroOperacionRecaudacion",
            peticion,
            "numeroOperacionRecaudacion"
        );

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

    private void agregar(
        StringBuilder xml,
        String nombre,
        String valor
    ) {

        if (valor != null && !valor.isBlank()) {

            xml.append("<uac:")
                .append(nombre)
                .append(">");

            xml.append(escapar(valor));

            xml.append("</uac:")
                .append(nombre)
                .append(">");
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

    @Override
    public RecaudacionResponse interpretar(
        RespuestaCruda respuesta,
        RecaudacionRequest peticion
    ) {

        LOG.debugf("respuesta UNIVCUSCO operador=%s", peticion.operador());

        return new RecaudacionResponse(
            Institucion.UNIVCUSCO,
            peticion.operador(),
            "00",
            "OK",
            Map.of()
        );
    }
}
