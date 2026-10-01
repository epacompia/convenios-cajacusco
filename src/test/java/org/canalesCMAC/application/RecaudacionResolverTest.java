package org.canalesCMAC.application;

import org.canalesCMAC.domain.exception.OperadorNoSoportadoException;
import org.canalesCMAC.domain.model.Institucion;
import org.canalesCMAC.domain.model.Operador;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecaudacionResolverTest {

    private final RecaudacionResolver resolver = new RecaudacionResolver();

    @Test
    void aceptaConsultaDeudaDeElse() {
        assertDoesNotThrow(() -> resolver.validar(Institucion.ELSE, Operador.CONSULTA_DEUDA));
    }

    @Test
    void aceptaExtornoAutomaticoDeElectroUcayali() {
        assertDoesNotThrow(() -> resolver.validar(Institucion.ELECTRO_UCAYALI, Operador.EXTORNO_AUTO));
    }

    @Test
    void rechazaConsultaPagoEnElectroUcayali() {
        assertThrows(OperadorNoSoportadoException.class,
            () -> resolver.validar(Institucion.ELECTRO_UCAYALI, Operador.CONSULTA_PAGO));
    }

    @Test
    void rechazaExternoAutomaticoEnElse() {
        assertThrows(OperadorNoSoportadoException.class,
            () -> resolver.validar(Institucion.ELSE, Operador.EXTORNO_PAGO_AUTO));
    }

    @Test
    void aceptaAnulacionDeSeal() {
        assertDoesNotThrow(() -> resolver.validar(Institucion.SEAL, Operador.ANULACION));
    }

    @Test
    void rechazaConsultaPagoEnSeal() {
        assertThrows(OperadorNoSoportadoException.class,
            () -> resolver.validar(Institucion.SEAL, Operador.CONSULTA_PAGO));
    }
}