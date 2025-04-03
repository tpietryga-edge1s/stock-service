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
import org.tobiaszpietryga.stock.doman.Product;
import org.tobiaszpietryga.stock.repository.ProductRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

	public static final String STOCK_ORDERS = "stock-orders";
	@Mock
	KafkaTemplate<Long, Order> kafkaTemplate;
	@InjectMocks
	StockService underTest;
	@Mock
	ProductRepository productRepository;

	@Captor
	ArgumentCaptor<Order> orderCaptor;
	@Captor
	ArgumentCaptor<Product> productArgumentCaptor;

	@BeforeEach
	void setUp() {
	}

	@Test
	void shouldReserveStock_whenNoPendingStockIsPresent() {
		//given
		Mockito.when(productRepository.findById(1L)).thenReturn(Optional.of(prepareProduct(20, 0)));
		ReflectionTestUtils.setField(underTest, "topicName", STOCK_ORDERS);

		//when
		underTest.reserveStock(prepareOrder(Status.NEW, false, 4));

		//then
		assertOrderSentToKafka(Status.PARTIALLY_CONFIRMED, Boolean.TRUE);

		assertProductItems(16, 4);
	}

	@Test
	void shouldRejectStock_whenNoPendingStockIsPresent() {
		//given
		Mockito.when(productRepository.findById(1L)).thenReturn(Optional.of(prepareProduct(20, 0)));
		ReflectionTestUtils.setField(underTest, "topicName", STOCK_ORDERS);

		//when
		underTest.reserveStock(prepareOrder(Status.NEW, false, 25));

		//then
		assertOrderSentToKafka(Status.PARTIALLY_REJECTED, Boolean.FALSE);

		Mockito.verify(productRepository, Mockito.never()).save(any());
	}

	@Test
	void shouldConfirmStock_whenNoPendingStockIsPresent() {
		//given
		Mockito.when(productRepository.findById(1L)).thenReturn(Optional.of(prepareProduct(16, 4)));
		ReflectionTestUtils.setField(underTest, "topicName", STOCK_ORDERS);

		//when
		underTest.confirmOrRollbackStock(prepareOrder(Status.CONFIRMED, true, 4));

		//then
		assertProductItems(16, 0);
	}

	@Test
	void shouldRollbackStock_whenNoPendingStockIsPresent() {
		//given
		Mockito.when(productRepository.findById(1L)).thenReturn(Optional.of(prepareProduct(16, 4)));
		ReflectionTestUtils.setField(underTest, "topicName", STOCK_ORDERS);

		//when
		underTest.confirmOrRollbackStock(prepareOrder(Status.ROLLBACK, true, 4));

		//then
		assertProductItems(20, 0);
	}

	private void assertProductItems(int availableItems, int reservedItems) {
		Mockito.verify(productRepository).save(productArgumentCaptor.capture());
		Product savedProduct = productArgumentCaptor.getValue();
		Assertions.assertThat(savedProduct.getItemsAvailable()).isEqualTo(availableItems);
		Assertions.assertThat(savedProduct.getItemsReserved()).isEqualTo(reservedItems);
	}

	private static Order prepareOrder(Status status, boolean stockStarted, int productCount) {
		return Order.builder()
				.id(1L)
				.status(status)
				.productId(1L)
				.stockStarted(stockStarted)
				.productCount(productCount)
				.build();
	}

	private static Product prepareProduct(int amountAvailable, int amountReserved) {
		return Product.builder()
				.itemsAvailable(amountAvailable)
				.itemsReserved(amountReserved)
				.id(1L)
				.build();
	}

	private void assertOrderSentToKafka(Status status, Boolean stockStarted) {
		Mockito.verify(kafkaTemplate).send(eq(STOCK_ORDERS), eq(1L), orderCaptor.capture());
		Order sentOrder = orderCaptor.getValue();
		Assertions.assertThat(sentOrder.isStockStarted()).isEqualTo(stockStarted);
		Assertions.assertThat(sentOrder.getStatus()).isEqualTo(status);
	}
}
