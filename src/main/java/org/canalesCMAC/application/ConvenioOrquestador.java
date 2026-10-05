package org.canalesCMAC.application;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.canalesCMAC.application.port.MapeadorOperacion;
import org.canalesCMAC.application.port.PeticionSalida;
import org.canalesCMAC.application.port.ProtocoloCliente;
import org.canalesCMAC.application.port.RespuestaCruda;
import org.canalesCMAC.domain.exception.InstitucionNoSoportadaException;
import org.canalesCMAC.domain.exception.ProveedorException;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;
import org.jboss.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ConvenioOrquestador {

    private static final Logger LOG = Logger.getLogger(ConvenioOrquestador.class);

    @Inject
    ConvenioRegistro registro;

    @Inject
    Instance<MapeadorOperacion> mapeadores;

    @Inject
    Instance<ProtocoloCliente> clientes;

    private Map<Institucion, MapeadorOperacion> mapeadoresPorInstitucion;
    private Map<String, ProtocoloCliente> clientesPorProtocolo;

    @PostConstruct
    void inicializar() {
        Map<Institucion, MapeadorOperacion> porInstitucion = new EnumMap<>(Institucion.class);
        for (MapeadorOperacion mapeador : mapeadores) {
            porInstitucion.put(mapeador.institucion(), mapeador);
        }
        this.mapeadoresPorInstitucion = porInstitucion;

        Map<String, ProtocoloCliente> porProtocolo = new HashMap<>();
        for (ProtocoloCliente cliente : clientes) {
            porProtocolo.put(cliente.protocolo(), cliente);
        }
        this.clientesPorProtocolo = porProtocolo;
        LOG.infof("orquestador inicializado mapeadores=%s protocolos=%s",
            mapeadoresPorInstitucion.keySet(), clientesPorProtocolo.keySet());
    }

    public RecaudacionResponse procesar(RecaudacionRequest peticion) {
        Convenio convenio = registro.convenio(peticion.institucion());
        if (convenio == null) {
            throw new InstitucionNoSoportadaException("Institución no soportada: " + peticion.institucion());
        }
        MapeadorOperacion mapeador = mapeadoresPorInstitucion.get(peticion.institucion());
        if (mapeador == null) {
            throw new InstitucionNoSoportadaException("Sin mapeador configurado para " + peticion.institucion());
        }
        ProtocoloCliente cliente = clientesPorProtocolo.get(convenio.protocolo());
        if (cliente == null) {
            throw new InstitucionNoSoportadaException(
                "Sin cliente para el protocolo " + convenio.protocolo() + " de " + peticion.institucion());
        }
        LOG.debugf("orquestando institucion=%s operador=%s protocolo=%s",
            peticion.institucion(), peticion.operador(), convenio.protocolo());
        PeticionSalida salida = mapeador.construir(peticion);
        RespuestaCruda respuesta = cliente.ejecutar(convenio, salida);
        try {
            return mapeador.interpretar(respuesta, peticion);
        } catch (ProveedorException excepcion) {
            throw excepcion;
        } catch (RuntimeException excepcion) {
            throw new ProveedorException(
                "Respuesta inválida de " + peticion.institucion() + ": " + excepcion.getMessage(),
                ProveedorException.BAD_GATEWAY, false, excepcion);
        }
    }
}
