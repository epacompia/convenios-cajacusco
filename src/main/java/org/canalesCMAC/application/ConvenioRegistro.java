package org.canalesCMAC.application;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.canalesCMAC.application.port.Autenticacion;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;
import org.eclipse.microprofile.config.Config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ConvenioRegistro {

    private static final String PREFIJO = "convenio.";

    private final Map<Institucion, Set<Operador>> operadores;
    private final Map<Institucion, String> protocolos;
    private final Map<Institucion, String> autenticaciones;
    private final Map<String, Autenticacion> autenticacionesPorTipo;

    @Inject
    public ConvenioRegistro(Config config, Instance<Autenticacion> autenticaciones) {
        this.operadores = parsearOperadores(config);
        this.protocolos = parsearProtocolos(config);
        this.autenticaciones = parsearAutenticaciones(config);
        this.autenticacionesPorTipo = porTipo(autenticaciones);
    }

    public ConvenioRegistro(Map<Institucion, Set<Operador>> operadores, Map<Institucion, String> protocolos) {
        this.operadores = operadores;
        this.protocolos = protocolos;
        this.autenticaciones = Map.of();
        this.autenticacionesPorTipo = Map.of();
    }

    public Set<Institucion> instituciones() {
        return Collections.unmodifiableSet(operadores.keySet());
    }

    public boolean soporta(Institucion institucion) {
        return operadores.containsKey(institucion);
    }

    public boolean soporta(Institucion institucion, Operador operador) {
        Set<Operador> soportados = operadores.get(institucion);
        return soportados != null && soportados.contains(operador);
    }

    public Set<Operador> operadores(Institucion institucion) {
        return operadores.getOrDefault(institucion, Set.of());
    }

    public String protocolo(Institucion institucion) {
        return protocolos.getOrDefault(institucion, "generic");
    }

    public Autenticacion autenticacion(Institucion institucion) {
        Autenticacion autenticacion = autenticacionesPorTipo.get(autenticaciones.getOrDefault(institucion, "ninguna"));
        return autenticacion == null ? Autenticacion.NINGUNA : autenticacion;
    }

    public String ruta(Institucion institucion) {
        return "direct:convenio." + institucion.name();
    }

    private static Map<Institucion, Set<Operador>> parsearOperadores(Config config) {
        Map<Institucion, Set<Operador>> resultado = new EnumMap<>(Institucion.class);
        for (String propiedad : config.getPropertyNames()) {
            if (!propiedad.startsWith(PREFIJO) || !propiedad.endsWith(".operadores")) {
                continue;
            }
            Institucion institucion = institucion(propiedad);
            if (institucion == null) {
                continue;
            }
            Set<Operador> soportados = EnumSet.noneOf(Operador.class);
            for (String operador : config.getValue(propiedad, String.class).split(",")) {
                if (!operador.isBlank()) {
                    soportados.add(Operador.valueOf(operador.trim()));
                }
            }
            resultado.put(institucion, soportados);
        }
        return resultado;
    }

    private static Map<Institucion, String> parsearProtocolos(Config config) {
        Map<Institucion, String> resultado = new EnumMap<>(Institucion.class);
        for (String propiedad : config.getPropertyNames()) {
            if (!propiedad.startsWith(PREFIJO) || !propiedad.endsWith(".protocolo")) {
                continue;
            }
            Institucion institucion = institucion(propiedad);
            if (institucion != null) {
                resultado.put(institucion, config.getValue(propiedad, String.class));
            }
        }
        return resultado;
    }

    private static Map<Institucion, String> parsearAutenticaciones(Config config) {
        Map<Institucion, String> resultado = new EnumMap<>(Institucion.class);
        for (String propiedad : config.getPropertyNames()) {
            if (!propiedad.startsWith(PREFIJO) || !propiedad.endsWith(".auth")) {
                continue;
            }
            Institucion institucion = institucion(propiedad);
            if (institucion != null) {
                resultado.put(institucion, config.getValue(propiedad, String.class));
            }
        }
        return resultado;
    }

    private static Map<String, Autenticacion> porTipo(Instance<Autenticacion> autenticaciones) {
        Map<String, Autenticacion> resultado = new HashMap<>();
        for (Autenticacion autenticacion : autenticaciones) {
            resultado.put(autenticacion.tipo(), autenticacion);
        }
        return resultado;
    }

    private static Institucion institucion(String propiedad) {
        String[] partes = propiedad.split("\\.");
        if (partes.length < 2) {
            return null;
        }
        for (Institucion institucion : Institucion.values()) {
            if (institucion.name().equalsIgnoreCase(partes[1])) {
                return institucion;
            }
        }
        return null;
    }
}
