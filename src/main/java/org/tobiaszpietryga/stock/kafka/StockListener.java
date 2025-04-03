package org.tobiaszpietryga.stock.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.tobiaszpietryga.order.common.model.Order;
import org.tobiaszpietryga.order.common.model.Status;
import org.tobiaszpietryga.stock.sevice.StockService;

@Slf4j
@RequiredArgsConstructor
@Service
public class StockListener {
	private final StockService stockService;
	@KafkaListener(id = "payment-service-order-listener", topics = "${orders.topic.name}", groupId = "payment-service-order-listener")
	public void onEvent(Order order) {
		log.info("Received: {}", order);
		if (order.getStatus().equals(Status.NEW)) {
			stockService.reservePayment(order);
		} else {
			stockService.confirmOrRollbackPayment(order);
		}
	}
}
