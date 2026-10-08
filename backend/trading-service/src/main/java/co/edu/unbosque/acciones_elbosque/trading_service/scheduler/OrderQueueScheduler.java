package co.edu.unbosque.acciones_elbosque.trading_service.scheduler;

import co.edu.unbosque.acciones_elbosque.trading_service.service.QueuedOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class OrderQueueScheduler {

    private final QueuedOrderService queuedOrderService;

    @Scheduled(fixedDelay = 60_000)
    public void tick() {
        log.debug("Checking for queued orders to process");
        queuedOrderService.processQueue();
    }
}
