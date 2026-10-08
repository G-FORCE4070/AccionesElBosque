package co.edu.unbosque.acciones_elbosque.trading_service.exception;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException() {
        super("Orden no encontrada");
    }
}
