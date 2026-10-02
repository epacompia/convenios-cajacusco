package org.canalesCMAC.adapter.in.rest;

import java.util.Map;

import org.canalesCMAC.domain.exception.RecaudacionException;
import org.jboss.logging.Logger;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class RecaudacionExceptionMapper implements ExceptionMapper<RecaudacionException> {

    private static final Logger LOG = Logger.getLogger(RecaudacionExceptionMapper.class);

    @Override
    public Response toResponse(RecaudacionException excepcion) {
        LOG.warnf("solicitud rechazada mensaje=%s", excepcion.getMessage());
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(Map.of("codigo", "ER", "mensaje", excepcion.getMessage()))
            .build();
    }
}