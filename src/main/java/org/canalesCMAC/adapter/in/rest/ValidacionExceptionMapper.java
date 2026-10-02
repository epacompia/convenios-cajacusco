package org.canalesCMAC.adapter.in.rest;

import java.util.Map;

import org.jboss.logging.Logger;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ValidacionExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    private static final Logger LOG = Logger.getLogger(ValidacionExceptionMapper.class);

    @Override
    public Response toResponse(IllegalArgumentException excepcion) {
        LOG.debugf("solicitud inválida mensaje=%s", excepcion.getMessage());
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(Map.of("codigo", "ER", "mensaje", excepcion.getMessage()))
            .build();
    }
}
