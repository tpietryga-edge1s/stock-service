package org.tobiaszpietryga.stock.sevice;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.tobiaszpietryga.order.common.model.Order;
import org.tobiaszpietryga.order.common.model.Status;
import org.tobiaszpietryga.stock.doman.Customer;
import org.tobiaszpietryga.stock.repository.CustomerRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

	public static final String PAYMENT_ORDERS = "payment-orders";
	@Mock
	KafkaTemplate<Long, Order> kafkaTemplate;
	@InjectMocks
	StockService underTest;
	@Mock
	CustomerRepository customerRepository;

	@Captor
	ArgumentCaptor<Order> orderCaptor;
	@Captor
	ArgumentCaptor<Customer> customerCaptor;

	@BeforeEach
	void setUp() {
	}

	@Test
	void shouldReservePayment_whenNoPendingPaymentIsPresent() {
		//given
		Mockito.when(customerRepository.findById(1L)).thenReturn(Optional.of(prepareCustomer(20, 0)));
		ReflectionTestUtils.setField(underTest, "topicName", PAYMENT_ORDERS);

		//when
		underTest.reservePayment(prepareOrder(Status.NEW, false, 4));

		//then
		assertOrderSentToKafka(Status.PARTIALLY_CONFIRMED, Boolean.TRUE);

		assertCustomerAmounts(16, 4);
	}

	@Test
	void shouldRejectPayment_whenNoPendingPaymentIsPresent() {
		//given
		Mockito.when(customerRepository.findById(1L)).thenReturn(Optional.of(prepareCustomer(20, 0)));
		ReflectionTestUtils.setField(underTest, "topicName", PAYMENT_ORDERS);

		//when
		underTest.reservePayment(prepareOrder(Status.NEW, false, 25));

		//then
		assertOrderSentToKafka(Status.PARTIALLY_REJECTED, Boolean.FALSE);

		Mockito.verify(customerRepository, Mockito.never()).save(any());
	}

	@Test
	void shouldConfirmPayment_whenNoPendingPaymentIsPresent() {
		//given
		Mockito.when(customerRepository.findById(1L)).thenReturn(Optional.of(prepareCustomer(16, 4)));
		ReflectionTestUtils.setField(underTest, "topicName", PAYMENT_ORDERS);

		//when
		underTest.confirmOrRollbackPayment(prepareOrder(Status.CONFIRMED, true, 4));

		//then
		assertCustomerAmounts(16, 0);
	}

	@Test
	void shouldRollbackPayment_whenNoPendingPaymentIsPresent() {
		//given
		Mockito.when(customerRepository.findById(1L)).thenReturn(Optional.of(prepareCustomer(16, 4)));
		ReflectionTestUtils.setField(underTest, "topicName", PAYMENT_ORDERS);

		//when
		underTest.confirmOrRollbackPayment(prepareOrder(Status.ROLLBACK, true, 4));

		//then
		assertCustomerAmounts(20, 0);
	}

	private void assertCustomerAmounts(int availableAmount, int reservedAmount) {
		Mockito.verify(customerRepository).save(customerCaptor.capture());
		Customer savedCustomer = customerCaptor.getValue();
		Assertions.assertThat(savedCustomer.getAmountAvailable()).isEqualTo(availableAmount);
		Assertions.assertThat(savedCustomer.getAmountReserved()).isEqualTo(reservedAmount);
	}

	private static Order prepareOrder(Status status, boolean paymentStarted, int price) {
		return Order.builder()
				.id(1L)
				.status(status)
				.customerId(1L)
				.paymentStarted(paymentStarted)
				.price(price)
				.build();
	}

	private static Customer prepareCustomer(int amountAvailable, int amountReserved) {
		return Customer.builder()
				.amountAvailable(amountAvailable)
				.amountReserved(amountReserved)
				.id(1L)
				.build();
	}

	private void assertOrderSentToKafka(Status status, Boolean paymentStarted) {
		Mockito.verify(kafkaTemplate).send(eq(PAYMENT_ORDERS), eq(1L), orderCaptor.capture());
		Order sentOrder = orderCaptor.getValue();
		Assertions.assertThat(sentOrder.isPaymentStarted()).isEqualTo(paymentStarted);
		Assertions.assertThat(sentOrder.getStatus()).isEqualTo(status);
	}
}
