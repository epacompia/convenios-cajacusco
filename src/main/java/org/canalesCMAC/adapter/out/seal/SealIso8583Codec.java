package org.canalesCMAC.adapter.out.seal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.canalesCMAC.domain.exception.OperadorNoSoportadoException;
import org.canalesCMAC.domain.model.Canonico;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;
import org.canalesCMAC.domain.model.RecaudacionRequest;
import org.canalesCMAC.domain.model.RecaudacionResponse;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SealIso8583Codec {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HHmmss");
    private static final String HEX = "0123456789ABCDEF";
    private static final Map<Integer, Integer> LONGITUDES = Map.ofEntries(
        Map.entry(2, 19), Map.entry(3, 6), Map.entry(4, 12), Map.entry(11, 6),
        Map.entry(12, 6), Map.entry(13, 8), Map.entry(22, 3), Map.entry(25, 2),
        Map.entry(32, 8), Map.entry(33, 8), Map.entry(37, 12), Map.entry(38, 6),
        Map.entry(39, 2), Map.entry(41, 8), Map.entry(42, 15), Map.entry(43, 40),
        Map.entry(49, 3));

    @ConfigProperty(name = "recaudacion.seal.usuario", defaultValue = "USUARIO")
    String usuarioBanco;

    @ConfigProperty(name = "recaudacion.seal.car", defaultValue = "9999")
    String car;

    @ConfigProperty(name = "recaudacion.seal.acquirer", defaultValue = "40001000")
    String acquirer;

    @ConfigProperty(name = "recaudacion.seal.forward", defaultValue = "20013005")
    String forward;

    @ConfigProperty(name = "recaudacion.seal.moneda", defaultValue = "604")
    String moneda;

    @ConfigProperty(name = "recaudacion.seal.agencia", defaultValue = "015")
    String agencia;

    @ConfigProperty(name = "recaudacion.seal.direccion", defaultValue = "")
    String direccion;

    public String construir(RecaudacionRequest peticion) {
        Operacion operacion = Operacion.de(peticion.operador());
        LocalDateTime fecha = fechaOperacion(peticion);
        String trace = trace(peticion);
        Map<Integer, String> campos = new LinkedHashMap<>();
        campos.put(2, padLeft("", 19));
        campos.put(3, operacion.processingCode);
        campos.put(4, operacion.conMonto ? monto(peticion) : padLeft("", 12));
        campos.put(11, trace);
        campos.put(12, HORA.format(fecha));
        campos.put(13, FECHA.format(fecha));
        campos.put(22, "010");
        campos.put(25, "10");
        campos.put(32, padLeft(acquirer, 8));
        campos.put(33, padLeft(forward, 8));
        campos.put(37, padLeft(trace, 12));
        if (operacion.requiereAprobacion) {
            campos.put(38, padLeft(aprobacion(peticion), 6));
        }
        campos.put(41, padLeft(car, 8));
        campos.put(42, padLeft(agencia, 15));
        campos.put(43, padRight(direccion, 40));
        campos.put(49, padLeft(moneda, 3));
        campos.put(121, campo121(peticion));
        return ensamblar(operacion.mti, campos);
    }

    public RecaudacionResponse interpretar(String trama, RecaudacionRequest peticion) {
        Map<Integer, String> campos = parsear(trama);
        String codigo = campos.get(39);
        if (codigo == null || codigo.isBlank()) {
            codigo = "99";
        }
        String mensaje = mensaje(codigo);
        Map<String, Object> datos = new LinkedHashMap<>();
        if ("00".equals(codigo)) {
            Map<String, String> privados = subcampos(campos.get(121));
            poner(datos, Canonico.IDENTIFICADOR_TRANSACCION, recortar(campos.get(11)));
            poner(datos, Canonico.CODIGO_APROBACION, recortar(campos.get(38)));
            poner(datos, Canonico.CONTRATO, privados.get("P02"));
            poner(datos, Canonico.NOMBRE_CLIENTE, privados.get("P04"));
            if (peticion.operador() == Operador.CONSULTA_DEUDA) {
                poner(datos, Canonico.MONTO_PAGAR, decimal(campos.get(4)));
                poner(datos, Canonico.DIRECCION_CLIENTE, privados.get("P05"));
            } else {
                poner(datos, Canonico.MONTO, decimal(campos.get(4)));
            }
        }
        return new RecaudacionResponse(Institucion.SEAL, peticion.operador(), codigo, mensaje, datos);
    }

    private String ensamblar(String mti, Map<Integer, String> campos) {
        Set<Integer> bits = new TreeSet<>(campos.keySet());
        boolean secundario = bits.stream().anyMatch(b -> b > 64);
        Set<Integer> primarios = new TreeSet<>();
        for (int bit : bits) {
            if (bit <= 64) {
                primarios.add(bit);
            }
        }
        if (secundario) {
            primarios.add(1);
        }
        StringBuilder trama = new StringBuilder();
        trama.append(mti);
        trama.append(bitmap(primarios, 0));
        if (secundario) {
            trama.append(bitmap(bits, 64));
        }
        for (int bit : bits) {
            trama.append(campos.get(bit));
        }
        return trama.toString();
    }

    private Map<Integer, String> parsear(String trama) {
        Map<Integer, String> campos = new LinkedHashMap<>();
        int pos = 4;
        Set<Integer> bits = bits(trama.substring(pos, pos + 16), 0);
        pos += 16;
        if (bits.contains(1)) {
            bits.addAll(bits(trama.substring(pos, pos + 16), 64));
            pos += 16;
        }
        for (int bit : bits) {
            if (bit == 1) {
                continue;
            }
            if (bit == 121) {
                int longitud = Integer.parseInt(trama.substring(pos, pos + 4));
                campos.put(bit, trama.substring(pos, pos + longitud));
                pos += longitud;
            } else {
                int longitud = LONGITUDES.getOrDefault(bit, 0);
                campos.put(bit, trama.substring(pos, pos + longitud));
                pos += longitud;
            }
        }
        return campos;
    }

    private String bitmap(Set<Integer> bits, int offset) {
        StringBuilder hex = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            int valor = 0;
            for (int j = 0; j < 4; j++) {
                if (bits.contains(offset + i * 4 + j + 1)) {
                    valor |= (8 >> j);
                }
            }
            hex.append(HEX.charAt(valor));
        }
        return hex.toString();
    }

    private Set<Integer> bits(String hex, int offset) {
        Set<Integer> bits = new TreeSet<>();
        for (int i = 0; i < 16; i++) {
            int valor = Character.digit(hex.charAt(i), 16);
            for (int j = 0; j < 4; j++) {
                if ((valor & (8 >> j)) != 0) {
                    bits.add(offset + i * 4 + j + 1);
                }
            }
        }
        return bits;
    }

    private Map<String, String> subcampos(String campo121) {
        Map<String, String> privados = new LinkedHashMap<>();
        if (campo121 == null || campo121.length() < 4) {
            return privados;
        }
        String contenido = campo121.substring(4);
        int pos = 0;
        pos = agregar(privados, contenido, pos, "P01", 7);
        pos = agregar(privados, contenido, pos, "P02", 6);
        pos = agregar(privados, contenido, pos, "P03", 4);
        pos = agregar(privados, contenido, pos, "P04", 50);
        agregar(privados, contenido, pos, "P05", 50);
        return privados;
    }

    private int agregar(Map<String, String> privados, String contenido, int pos, String nombre, int longitud) {
        if (pos + longitud <= contenido.length()) {
            String valor = recortar(contenido.substring(pos, pos + longitud));
            if (!valor.isBlank()) {
                privados.put(nombre, valor);
            }
        }
        return pos + longitud;
    }

    private String campo121(RecaudacionRequest peticion) {
        StringBuilder contenido = new StringBuilder();
        contenido.append(padRight(usuarioBanco, 7));
        contenido.append(padRight(contrato(peticion), 6));
        contenido.append(padLeft(car, 4));
        contenido.append(tipoPago(peticion));
        int longitud = 4 + contenido.length();
        return padLeft(String.valueOf(longitud), 4) + contenido;
    }

    private String contrato(RecaudacionRequest peticion) {
        Object valor = peticion.datos().get(Canonico.CONTRATO);
        if (valor == null || valor.toString().isBlank()) {
            valor = peticion.datos().get(Canonico.NUMERO_SUMINISTRO);
        }
        if (valor == null || valor.toString().isBlank()) {
            throw new IllegalArgumentException("Campo canónico obligatorio ausente: " + Canonico.CONTRATO);
        }
        return valor.toString();
    }

    private String tipoPago(RecaudacionRequest peticion) {
        Object valor = peticion.datos().get(Canonico.TIPO_PAGO);
        return valor == null || valor.toString().isBlank() ? "1" : valor.toString();
    }

    private String aprobacion(RecaudacionRequest peticion) {
        Object valor = peticion.datos().get(Canonico.CODIGO_APROBACION);
        if (valor == null || valor.toString().isBlank()) {
            throw new IllegalArgumentException("Campo canónico obligatorio ausente: " + Canonico.CODIGO_APROBACION);
        }
        return valor.toString();
    }

    private String monto(RecaudacionRequest peticion) {
        Object valor = peticion.datos().get(Canonico.MONTO);
        if (valor == null) {
            throw new IllegalArgumentException("Campo canónico obligatorio ausente: monto");
        }
        return padLeft(new BigDecimal(valor.toString()).movePointRight(2).toBigInteger().toString(), 12);
    }

    private String trace(RecaudacionRequest peticion) {
        Object valor = peticion.datos().get(Canonico.IDENTIFICADOR_TRANSACCION);
        String base = valor == null ? "" : valor.toString().replaceAll("\\D", "");
        if (base.length() > 6) {
            base = base.substring(base.length() - 6);
        }
        if (base.isBlank()) {
            base = String.valueOf(System.currentTimeMillis() % 1_000_000L);
        }
        return padLeft(base, 6);
    }

    private LocalDateTime fechaOperacion(RecaudacionRequest peticion) {
        Object valor = peticion.datos().get(Canonico.FECHA_OPERACION);
        if (valor != null && !valor.toString().isBlank()) {
            return LocalDateTime.parse(valor.toString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        return LocalDateTime.now();
    }

    private String mensaje(String codigo) {
        return switch (codigo) {
            case "00" -> "TRANSACCION CORRECTA";
            case "06" -> "FORMATO DE MENSAJE INVALIDO";
            case "07" -> "CONTRATO NO ESTA DISPONIBLE PARA SER PAGADO";
            case "20" -> "SIN PAGO PARA EXTORNAR";
            default -> "";
        };
    }

    private BigDecimal decimal(String valor) {
        return valor == null || valor.isBlank() ? null : new BigDecimal(valor).movePointLeft(2);
    }

    private void poner(Map<String, Object> destino, String clave, Object valor) {
        if (valor != null) {
            destino.put(clave, valor);
        }
    }

    private String recortar(String valor) {
        return valor == null ? null : valor.trim();
    }

    private String padLeft(String valor, int longitud) {
        String v = valor == null ? "" : valor;
        if (v.length() >= longitud) {
            return v.substring(v.length() - longitud);
        }
        return "0".repeat(longitud - v.length()) + v;
    }

    private String padRight(String valor, int longitud) {
        String v = valor == null ? "" : valor;
        if (v.length() >= longitud) {
            return v.substring(0, longitud);
        }
        return v + " ".repeat(longitud - v.length());
    }

    private enum Operacion {
        CONSULTA(Operador.CONSULTA_DEUDA, "0200", "310000", false, false),
        PAGO(Operador.PAGO_DEUDA, "0200", "210000", true, false),
        ANULACION(Operador.ANULACION, "0200", "220000", false, true),
        EXTORNO(Operador.EXTORNO_PAGO, "0400", "210000", false, false);

        private final Operador operador;
        private final String mti;
        private final String processingCode;
        private final boolean conMonto;
        private final boolean requiereAprobacion;

        Operacion(Operador operador, String mti, String processingCode, boolean conMonto, boolean requiereAprobacion) {
            this.operador = operador;
            this.mti = mti;
            this.processingCode = processingCode;
            this.conMonto = conMonto;
            this.requiereAprobacion = requiereAprobacion;
        }

        private static Operacion de(Operador operador) {
            for (Operacion operacion : values()) {
                if (operacion.operador == operador) {
                    return operacion;
                }
            }
            throw new OperadorNoSoportadoException("Operador " + operador + " no soportado por SEAL");
        }
    }
}
