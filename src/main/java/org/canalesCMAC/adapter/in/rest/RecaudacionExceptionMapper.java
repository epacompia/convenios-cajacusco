package org.canalesCMAC.adapter.in.rest;

import java.util.Map;

import org.canalesCMAC.domain.exception.RecaudacionException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class RecaudacionExceptionMapper implements ExceptionMapper<RecaudacionException> {

    @Override
    public Response toResponse(RecaudacionException excepcion) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(Map.of("codigo", "ER", "mensaje", excepcion.getMessage()))
            .build();
    }
}