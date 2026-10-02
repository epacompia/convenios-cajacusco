package org.canalesCMAC.adapter.in.rest;

import java.util.Map;

import org.canalesCMAC.domain.exception.ProveedorException;
import org.jboss.logging.Logger;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ProveedorExceptionMapper implements ExceptionMapper<ProveedorException> {

    private static final Logger LOG = Logger.getLogger(ProveedorExceptionMapper.class);

    @Override
    public Response toResponse(ProveedorException excepcion) {
        LOG.errorf("fallo de proveedor status=%d mensaje=%s", excepcion.status(), excepcion.getMessage());
        return Response.status(excepcion.status())
            .entity(Map.of("codigo", "ER", "mensaje", excepcion.getMessage()))
            .build();
    }
}
