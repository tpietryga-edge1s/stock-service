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
import org.tobiaszpietryga.stock.repository.ProductRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {
	private final ProductRepository productRepository;
	private final KafkaTemplate<Long, Order> kafkaTemplate;
	@Value("${stock-orders.topic.name}")
	private String topicName;

	public void reserveStock(Order order) {
		Optional<Product> possibleProduct = productRepository.findById(order.getProductId());
		if (possibleProduct.isPresent()) {
			Product product = possibleProduct.get();
			log.info("Reserve: Found product: {}", product);
			if (order.getProductCount().compareTo(product.getItemsAvailable()) <= 0) {
				order.setStatus(Status.PARTIALLY_CONFIRMED);
				product.setItemsReserved(product.getItemsReserved() + order.getProductCount());
				product.setItemsAvailable(product.getItemsAvailable() - order.getProductCount());
				productRepository.save(product);
				log.info("Reserve: product saved: {}", product);
				order.setStockStarted(true);
			} else {
				order.setStatus(Status.PARTIALLY_REJECTED);
			}
		} else {
			order.setStatus(Status.PARTIALLY_REJECTED);
		}
		kafkaTemplate.send(topicName, order.getId(), order);
		log.info("Reserve: message sent: {}", order);
	}

	public void confirmOrRollbackStock(Order order) {
		Optional<Product> possibleProduct = productRepository.findById(order.getProductId());
		if (possibleProduct.isPresent()) {
			Product product = possibleProduct.get();
			log.info("Confirm: Found product: {}", product);
			if (order.getStatus().equals(Status.CONFIRMED)) {
				product.setItemsReserved(product.getItemsReserved() - order.getProductCount());
				productRepository.save(product);
				log.info("Confirm: confirm: product saved {}", product);
			} else if (order.getStatus().equals(Status.ROLLBACK)) {
				if (order.isStockStarted()) {
					product.setItemsReserved(product.getItemsReserved() - order.getProductCount());
					product.setItemsAvailable(product.getItemsAvailable() + order.getProductCount());
					productRepository.save(product);
					log.info("Confirm: rollback: product saved {}", product);
				} else {
					log.info("Confirm: rollback ignored");
				}
			} else {
				log.warn("Confirm: incorrect order status: {}", order.getStatus());
			}
		}
	}
}
