package org.canalesCMAC.infrastructure;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import java.util.HashMap;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class ConveniosWireMockResource implements QuarkusTestResourceLifecycleManager {

    private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
    private static final String NAMESPACE = "http://tempuri.org/";

    private WireMockServer elseServer;
    private WireMockServer euServer;
    private WireMockServer sealServer;

    @Override
    public Map<String, String> start() {
        elseServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        euServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        sealServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        elseServer.start();
        euServer.start();
        sealServer.start();
        configurarElse();
        configurarEu();
        configurarSeal();

        Map<String, String> propiedades = new HashMap<>();
        propiedades.put("convenio.ELSE.url",
            "http://localhost:" + elseServer.port() + "/wApiCobranzaLinea/SCobranza.svc");
        propiedades.put("convenio.ELECTRO_UCAYALI.url", "http://localhost:" + euServer.port());
        propiedades.put("convenio.SEAL.url", "http://localhost:" + sealServer.port() + "/seal");
        return propiedades;
    }

    @Override
    public void stop() {
        if (elseServer != null) {
            elseServer.stop();
        }
        if (euServer != null) {
            euServer.stop();
        }
        if (sealServer != null) {
            sealServer.stop();
        }
    }

    private void configurarElse() {
        stubElseConsultaDeuda();
        stubElsePagoDeuda();
        stubElseExtornoPago();
    }

    private void stubElseConsultaDeuda() {
        String respuesta = soap("ConsultaDeudaResponse",
            "<CodigoMensajeRetornoConsulta>0</CodigoMensajeRetornoConsulta>" +
            "<MensajeRetornoConsulta>CORRECTO</MensajeRetornoConsulta>" +
            "<IdentificadorTransaccion>TX-100</IdentificadorTransaccion>" +
            "<CodigoComprobante>2018001000000294383</CodigoComprobante>" +
            "<CodigoSuministro>10010681504</CodigoSuministro>" +
            "<NombreCliente>JUAN PEREZ</NombreCliente>" +
            "<NumeroDocumento>20123456789</NumeroDocumento>" +
            "<MontoAPagarConsulta>23.10</MontoAPagarConsulta>" +
            "<FechaEmision>2026-09-10</FechaEmision>" +
            "<FechaVencimiento>2026-10-10</FechaVencimiento>" +
            "<FechaVencimientoRSP>2026-10-10</FechaVencimientoRSP>");
        elseServer.stubFor(post(urlPathEqualTo("/wApiCobranzaLinea/SCobranza.svc"))
            .withRequestBody(containing("<els:ConsultaDeuda>"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "text/xml; charset=utf-8")
                .withBody(respuesta)));
    }

    private void stubElsePagoDeuda() {
        String respuesta = soap("PagoDeudaResponse",
            "<CodigoMensajeRetornoPago>0</CodigoMensajeRetornoPago>" +
            "<MensajeRetornoPago>CORRECTO</MensajeRetornoPago>" +
            "<IdentificadorTransaccion>TX-100</IdentificadorTransaccion>" +
            "<CodigoIdentificadorPago>20180425001000003</CodigoIdentificadorPago>" +
            "<CodigoComprobante>2018001000000294383</CodigoComprobante>" +
            "<CodigoSuministro>10010681504</CodigoSuministro>" +
            "<MontoPago>23.10</MontoPago>" +
            "<FechaRegistroPago>2026-09-22 22:30:00</FechaRegistroPago>");
        elseServer.stubFor(post(urlPathEqualTo("/wApiCobranzaLinea/SCobranza.svc"))
            .withRequestBody(containing("<els:PagoDeuda>"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "text/xml; charset=utf-8")
                .withBody(respuesta)));
    }

    private void stubElseExtornoPago() {
        String respuesta = soap("ExtornoPagoResponse",
            "<CodigoMensajeRetornoExtorno>0</CodigoMensajeRetornoExtorno>" +
            "<MensajeRetornoExtorno>CORRECTO</MensajeRetornoExtorno>" +
            "<IdentificadorTransaccion>TX-100</IdentificadorTransaccion>" +
            "<CodigoIdentificadorExtorno>201804250010000037</CodigoIdentificadorExtorno>" +
            "<CodigoComprobante>2018001000000294383</CodigoComprobante>" +
            "<CodigoSuministro>10010681504</CodigoSuministro>" +
            "<MontoExtorno>23.10</MontoExtorno>" +
            "<FechaRegistroExtorno>2026-09-22 22:31:00</FechaRegistroExtorno>");
        elseServer.stubFor(post(urlPathEqualTo("/wApiCobranzaLinea/SCobranza.svc"))
            .withRequestBody(containing("<els:ExtornoPago>"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "text/xml; charset=utf-8")
                .withBody(respuesta)));
    }

    private void configurarEu() {
        euServer.stubFor(post(urlPathEqualTo("/api/v1/electro/auth"))
            .willReturn(okJson("{\"token\":\"tkn-e2e-123\",\"Expires_in\":\"86400\"}")));
        euServer.stubFor(post(urlPathEqualTo("/api/v2/electro/consulta"))
            .willReturn(okJson("{\"codigo\":\"00\",\"mensaje\":\"CORRECTO\",\"nroSumin\":\"800006812\","
                + "\"nombreCliente\":\"JUAN PEREZ\",\"lstdebt\":[{\"numFactura\":\"201800100000\","
                + "\"montoDeuda\":19423.00,\"fechaEmision\":\"2026-09-10\","
                + "\"fechaVencimiento\":\"2026-10-10\",\"glosa\":\"RECIBO LUZ\"}]}")));
        euServer.stubFor(post(urlPathEqualTo("/api/v1/electro/pago"))
            .willReturn(okJson("{\"codigo\":\"00\",\"mensaje\":\"CORRECTO\",\"nroSumin\":\"800006812\","
                + "\"numFactura\":\"201800100000\",\"numOperacionEmpresa\":\"654321\"}")));
        euServer.stubFor(post(urlPathEqualTo("/api/v1/electro/extorno"))
            .willReturn(aResponse().withStatus(500).withBody("falla simulada")));
        euServer.stubFor(post(urlPathEqualTo("/api/v1/electro/extorno_automatico_pago"))
            .willReturn(okJson("{\"codigo\":\"00\",\"mensaje\":\"CORRECTO\"}")));
        euServer.stubFor(post(urlPathEqualTo("/api/v1/electro/extorno_automatico"))
            .willReturn(okJson("{\"codigo\":\"00\",\"mensaje\":\"CORRECTO\"}")));
    }

    private void configurarSeal() {
        sealServer.stubFor(post(urlPathEqualTo("/seal"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "text/plain; charset=ISO-8859-1")
                .withBody(sealConsultaResponse())));
    }

    private String sealConsultaResponse() {
        String contenido = padRight("INTERBA", 7) + padRight("98787", 6) + padLeft("9999", 4)
            + padRight("DELGADO DE VIZCARRA NORA", 50) + padRight("CALLE NICOLAS DE PIEROLA NRO 117-2", 50);
        String campo121 = padLeft(String.valueOf(contenido.length() + 4), 4) + contenido;
        return "0210" + "F03804818E808000" + "0000000000000080"
            + padLeft("", 19) + "310000" + "000000150000" + "369571" + "123846" + "20100416"
            + "010" + "10" + "40001000" + "20013005" + "123456789012" + "000001" + "00"
            + "00000000" + "604" + campo121;
    }

    private String padLeft(String valor, int longitud) {
        return "0".repeat(Math.max(0, longitud - valor.length())) + valor;
    }

    private String padRight(String valor, int longitud) {
        return valor + " ".repeat(Math.max(0, longitud - valor.length()));
    }

    private String soap(String operacionRespuesta, String contenido) {
        return XML_HEADER + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soap:Body><" + operacionRespuesta + " xmlns=\"" + NAMESPACE + "\">" + contenido
            + "</" + operacionRespuesta + "></soap:Body></soap:Envelope>";
    }
}