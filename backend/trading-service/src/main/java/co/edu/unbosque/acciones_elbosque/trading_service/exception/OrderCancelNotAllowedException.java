package co.edu.unbosque.acciones_elbosque.trading_service.exception;

public class OrderCancelNotAllowedException extends RuntimeException {
    public OrderCancelNotAllowedException() {
        super("Solo se pueden cancelar órdenes en estado PENDING_QUEUE");
    }
}
