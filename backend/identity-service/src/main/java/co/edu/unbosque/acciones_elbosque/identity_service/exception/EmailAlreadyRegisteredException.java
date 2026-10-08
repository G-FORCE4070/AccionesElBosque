package co.edu.unbosque.acciones_elbosque.identity_service.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException() {
        super("El correo electrónico ya está registrado");
    }
}
