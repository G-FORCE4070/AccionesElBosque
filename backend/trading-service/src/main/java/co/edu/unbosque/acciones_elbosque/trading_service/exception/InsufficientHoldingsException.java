package co.edu.unbosque.acciones_elbosque.trading_service.exception;

public class InsufficientHoldingsException extends RuntimeException {
    public InsufficientHoldingsException() {
        super("Cantidad insuficiente de acciones");
    }
}
