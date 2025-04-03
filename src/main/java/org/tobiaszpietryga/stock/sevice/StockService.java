package org.tobiaszpietryga.stock.sevice;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.tobiaszpietryga.order.common.model.Order;
import org.tobiaszpietryga.order.common.model.Status;
import org.tobiaszpietryga.stock.doman.Customer;
import org.tobiaszpietryga.stock.repository.CustomerRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {
	private final CustomerRepository customerRepository;
	private final KafkaTemplate<Long, Order> kafkaTemplate;
	@Value("${payment-orders.topic.name}")
	private String topicName;

	public void reservePayment(Order order) {
		Optional<Customer> possibleCustomer = customerRepository.findById(order.getCustomerId());
		if (possibleCustomer.isPresent()) {
			Customer customer = possibleCustomer.get();
			log.info("Reserve: Found customer: {}", customer);
			if (order.getPrice().compareTo(customer.getAmountAvailable()) <= 0) {
				order.setStatus(Status.PARTIALLY_CONFIRMED);
				customer.setAmountReserved(customer.getAmountReserved() + order.getPrice());
				customer.setAmountAvailable(customer.getAmountAvailable() - order.getPrice());
				customerRepository.save(customer);
				log.info("Reserve: customer saved: {}", customer);
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
		Optional<Customer> possibleCustomer = customerRepository.findById(order.getCustomerId());
		if (possibleCustomer.isPresent()) {
			Customer customer = possibleCustomer.get();
			log.info("Confirm: Found customer: {}", customer);
			if (order.getStatus().equals(Status.CONFIRMED)) {
				customer.setAmountReserved(customer.getAmountReserved() - order.getPrice());
				customerRepository.save(customer);
				log.info("Confirm: customer saved{}", customer);
			} else if (order.getStatus().equals(Status.ROLLBACK)) {
				customer.setAmountReserved(customer.getAmountReserved() - order.getPrice());
				customer.setAmountAvailable(customer.getAmountAvailable() + order.getPrice());
				customerRepository.save(customer);
				log.info("Confirm: customer saved{}", customer);
			} else {
				log.warn("Confirm: incorrect order status: {}", order.getStatus());
			}
		}
	}
}
