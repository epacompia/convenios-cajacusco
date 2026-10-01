package org.canalesCMAC.adapter.in.rest;

import java.util.Map;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ValidacionExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException excepcion) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(Map.of("codigo", "ER", "mensaje", excepcion.getMessage()))
            .build();
    }
}
