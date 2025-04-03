package org.tobiaszpietryga.stock.sevice;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.tobiaszpietryga.order.common.model.Order;
import org.tobiaszpietryga.order.common.model.Status;
import org.tobiaszpietryga.stock.doman.Product;
import org.tobiaszpietryga.stock.repository.CustomerRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {
	private final CustomerRepository customerRepository;
	private final KafkaTemplate<Long, Order> kafkaTemplate;
	@Value("${payment-orders.topic.name}")
	private String topicName;

	public void reserveStock(Order order) {
		Optional<Product> possibleCustomer = customerRepository.findById(order.getProductId());
		if (possibleCustomer.isPresent()) {
			Product product = possibleCustomer.get();
			log.info("Reserve: Found customer: {}", product);
			if (order.getProductCount().compareTo(product.getItemsAvailable()) <= 0) {
				order.setStatus(Status.PARTIALLY_CONFIRMED);
				product.setItemsReserved(product.getItemsReserved() + order.getProductCount());
				product.setItemsAvailable(product.getItemsAvailable() - order.getProductCount());
				customerRepository.save(product);
				log.info("Reserve: customer saved: {}", product);
				order.setPaymentStarted(true);
			} else {
				order.setStatus(Status.PARTIALLY_REJECTED);
			}
		} else {
			order.setStatus(Status.PARTIALLY_REJECTED);
		}
		kafkaTemplate.send(topicName, order.getId(), order);
		log.info("Reserve: message sent: {}", order);
	}

	public void confirmOrRollbackPayment(Order order) {
		Optional<Product> possibleCustomer = customerRepository.findById(order.getProductId());
		if (possibleCustomer.isPresent()) {
			Product product = possibleCustomer.get();
			log.info("Confirm: Found customer: {}", product);
			if (order.getStatus().equals(Status.CONFIRMED)) {
				product.setItemsReserved(product.getItemsReserved() - order.getProductCount());
				customerRepository.save(product);
				log.info("Confirm: customer saved{}", product);
			} else if (order.getStatus().equals(Status.ROLLBACK)) {
				product.setItemsReserved(product.getItemsReserved() - order.getProductCount());
				product.setItemsAvailable(product.getItemsAvailable() + order.getProductCount());
				customerRepository.save(product);
				log.info("Confirm: customer saved{}", product);
			} else {
				log.warn("Confirm: incorrect order status: {}", order.getStatus());
			}
		}
	}
}
