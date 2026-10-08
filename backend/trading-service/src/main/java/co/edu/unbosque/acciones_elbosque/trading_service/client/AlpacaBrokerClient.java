package co.edu.unbosque.acciones_elbosque.trading_service.client;

import co.edu.unbosque.acciones_elbosque.trading_service.dto.AlpacaOrderRequest;
import co.edu.unbosque.acciones_elbosque.trading_service.dto.AlpacaOrderResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AlpacaBrokerClient {

    private final RestClient alpacaBrokerClient;

    public AlpacaBrokerClient(@Qualifier("alpacaBrokerRestClient") RestClient alpacaBrokerClient) {
        this.alpacaBrokerClient = alpacaBrokerClient;
    }

    public AlpacaOrderResponse placeOrder(String accountId, AlpacaOrderRequest request) {
        return alpacaBrokerClient.post()
                .uri("/v1/trading/accounts/{accountId}/orders", accountId)
                .body(request)
                .retrieve()
                .body(AlpacaOrderResponse.class);
    }

    public AlpacaOrderResponse getOrderStatus(String accountId, String orderId) {
        return alpacaBrokerClient.get()
                .uri("/v1/trading/accounts/{accountId}/orders/{orderId}", accountId, orderId)
                .retrieve()
                .body(AlpacaOrderResponse.class);
    }
}
