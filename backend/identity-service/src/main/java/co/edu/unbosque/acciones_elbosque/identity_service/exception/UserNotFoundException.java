package co.edu.unbosque.acciones_elbosque.identity_service.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException() {
        super("Usuario no encontrado");
    }
}
