package co.edu.unbosque.acciones_elbosque.identity_service.exception;

public class MfaResendLimitException extends RuntimeException {
    public MfaResendLimitException(String message) {
        super(message);
    }
}
