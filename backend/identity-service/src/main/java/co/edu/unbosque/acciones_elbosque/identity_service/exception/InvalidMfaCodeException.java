package co.edu.unbosque.acciones_elbosque.identity_service.exception;

public class InvalidMfaCodeException extends RuntimeException {
    public InvalidMfaCodeException(String message) {
        super(message);
    }
}
