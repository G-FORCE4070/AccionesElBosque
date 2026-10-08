package co.edu.unbosque.acciones_elbosque.identity_service.exception;

public class MfaSessionNotFoundException extends RuntimeException {
    public MfaSessionNotFoundException(String message) {
        super(message);
    }
}
