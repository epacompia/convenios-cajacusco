package org.canalesCMAC.adapter.out.yaganaste;

import java.util.LinkedHashMap;
import java.util.List;
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
public class YaGanasteSoapMessageCodec implements MapeadorOperacion {

    private static final Logger LOG = Logger.getLogger(YaGanasteSoapMessageCodec.class);
    private static final String CONTENT_TYPE = "text/xml; charset=utf-8";

    private static final String SOAP_NS =
        "http://schemas.xmlsoap.org/soap/envelope/";

    private static final String SIGMA_NS ="net.sigma.h2h.ws";

    @Override
    public Institucion institucion() {
        return Institucion.YAGANASTE;
    }

    @Override
    public PeticionSalida construir(RecaudacionRequest peticion) {
        LOG.debugf("construyendo SOAP YAGANASTE operador=%s", peticion.operador());
        Map<String, Object> campos = campos(peticion);

        StringBuilder xml = new StringBuilder();

        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<soapenv:Envelope")
            .append(" xmlns:soapenv=\"")
            .append(SOAP_NS)
            .append("\"")
            .append(" xmlns:net=\"")
            .append(SIGMA_NS)
            .append("\">");

        xml.append("<soapenv:Header/>");
        xml.append("<soapenv:Body>");
        xml.append("<net:echo>");

        // Datos principales
        agregar(xml,"tpv",campos.get("tpv"));
        agregar(xml,"claveTpv",campos.get("claveTpv"));
        agregar(xml,"codigoProducto",campos.get("codigoProducto"));
        agregar(xml,"referenciaOperacion",campos.get("referenciaOperacion"));
        agregar(xml,"fechaHoraOperacion",campos.get("fechaHoraOperacion"));

        // parametrosOperacion
        agregarParametrosOperacion(xml,campos.get("parametrosOperacion"));

        xml.append("</net:echo>");
        xml.append("</soapenv:Body>");
        xml.append("</soapenv:Envelope>");
        return new PeticionSalida(xml.toString(), CONTENT_TYPE);
    }

    private Map<String, Object> campos(RecaudacionRequest peticion) {

        Map<String, Object> campos = new LinkedHashMap<>();

        poner(campos,"tpv",peticion,"tpv");
        poner(campos,"claveTpv",peticion,"claveTpv");
        poner(campos,"codigoProducto",peticion,"codigoProducto");
        poner(campos,"referenciaOperacion",peticion,"referenciaOperacion");
        poner(campos,"fechaHoraOperacion",peticion,"fechaHoraOperacion");

        Object parametros = peticion.datos().get("parametrosOperacion");

        if (parametros != null) {
            campos.put(
                "parametrosOperacion",
                parametros
            );
        }

        return campos;
    }

    private void poner(
        Map<String, Object> campos,
        String nombre,
        RecaudacionRequest peticion,
        String clave
    ) {

        Object valor =
            peticion.datos().get(clave);

        if (valor != null) {
            campos.put(
                nombre,
                valor
            );
        }
    }

    private void agregar(
        StringBuilder xml,
        String nombre,
        Object valor
    ) {

        if (valor == null) {
            return;
        }

        String texto = valor.toString();

        if (texto.isBlank()) {
            return;
        }

        xml.append("<")
            .append(nombre)
            .append(">");

        xml.append(escapar(texto));

        xml.append("</")
            .append(nombre)
            .append(">");
    }

    @SuppressWarnings("unchecked")
    private void agregarParametrosOperacion(
        StringBuilder xml,
        Object valor
    ) {

        if (valor == null) {
            return;
        }

        if (!(valor instanceof List<?> lista)) {
            return;
        }

        for (Object item : lista) {

            if (!(item instanceof Map<?, ?> parametro)) {
                continue;
            }

            xml.append("<parametrosOperacion>");

            Object indice =
                parametro.get("indice");

            Object valorParametro =
                parametro.get("valor");

            if (indice != null) {
                xml.append("<indice>")
                    .append(escapar(indice.toString()))
                    .append("</indice>");
            }

            if (valorParametro != null) {

                xml.append("<valor>")
                    .append(escapar(valorParametro.toString()))
                    .append("</valor>");
            }

            xml.append("</parametrosOperacion>");
        }
    }

    private String escapar(
        String valor
    ) {

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

        LOG.debugf("respuesta YAGANASTE operador=%s", peticion.operador());

        return new RecaudacionResponse(
            Institucion.YAGANASTE,
            peticion.operador(),
            "00",
            "OK",
            Map.of()
        );
    }
}