package org.canalesCMAC.domain.exception;

public class ProveedorException extends RuntimeException {

    public static final int BAD_GATEWAY = 502;
    public static final int GATEWAY_TIMEOUT = 504;

    private final int status;
    private final boolean reintentable;

    public ProveedorException(String mensaje, int status, boolean reintentable, Throwable causa) {
        super(mensaje, causa);
        this.status = status;
        this.reintentable = reintentable;
    }

    public int status() {
        return status;
    }

    public boolean reintentable() {
        return reintentable;
    }
}
