package co.edu.unbosque.acciones_elbosque.trading_service.exception;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException() {
        super("Fondos insuficientes");
    }
}
