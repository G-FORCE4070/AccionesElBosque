package co.edu.unbosque.acciones_elbosque.trading_service.exception;

public class MarketClosedException extends RuntimeException {
    public MarketClosedException() {
        super("El mercado está cerrado");
    }
}
