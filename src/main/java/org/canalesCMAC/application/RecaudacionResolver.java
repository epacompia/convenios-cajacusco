package org.canalesCMAC.application;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.canalesCMAC.domain.exception.InstitucionNoSoportadaException;
import org.canalesCMAC.domain.exception.OperadorNoSoportadoException;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecaudacionResolver {

    private static final Map<Institucion, Set<Operador>> OPERADORES_POR_INSTITUCION =
        Map.of(
            Institucion.ELSE,
            EnumSet.of(Operador.CONSULTA_DEUDA, Operador.PAGO_DEUDA, Operador.CONSULTA_PAGO, Operador.EXTORNO_PAGO),
            Institucion.ELECTRO_UCAYALI,
            EnumSet.of(Operador.CONSULTA_DEUDA, Operador.PAGO_DEUDA, Operador.EXTORNO_PAGO,
                    Operador.EXTORNO_PAGO_AUTO, Operador.EXTORNO_AUTO),
            Institucion.SEAL,
            EnumSet.of(Operador.CONSULTA_DEUDA, Operador.PAGO_DEUDA,
                    Operador.EXTORNO_PAGO, Operador.ANULACION)
            ,

            Institucion.CLARO,
            EnumSet.of(
                Operador.PAGO_DEUDA
            ),

            Institucion.UNIVCUSCO,
            EnumSet.of(
                Operador.CONSULTA_DEUDA
            ),
            
            Institucion.YAGANASTE,
            EnumSet.of(
                Operador.CONSULTA,
                Operador.SALDO
            ),

            Institucion.MUNICIPALIDAD_CUSCO,
            EnumSet.of(
                Operador.CONSULTA_DEUDA
            )
            );

    public void validar(Institucion institucion, Operador operador) {
        Set<Operador> soportados = OPERADORES_POR_INSTITUCION.get(institucion);
        if (soportados == null) {
            throw new InstitucionNoSoportadaException("Institución no soportada: " + institucion);
        }
        if (!soportados.contains(operador)) {
            throw new OperadorNoSoportadoException("Operador " + operador + " no soportado para " + institucion);
        }
    }
}