package org.tobiaszpietryga.stock.sevice;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.tobiaszpietryga.order.common.model.Order;
import org.tobiaszpietryga.order.common.model.Status;
import org.tobiaszpietryga.stock.doman.Product;
import org.tobiaszpietryga.stock.repository.ProductRepository;

import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(properties = { "spring.kafka.consumer.auto-offset-reset=earliest" })
@EmbeddedKafka(topics = { "stock-orders" },
		partitions = 1,
		bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext
public class StockComponentTest {
	@Autowired
	KafkaTemplate<Long, Order> kafkaTemplate;
	@Autowired
	ProductRepository productRepository;

	@Test
	void shouldPartiallyConfirmOrder() throws InterruptedException {
		//given
		//when
		kafkaTemplate.send("orders", 1L, prepareOrder(Status.NEW, true, 5, 1L));

		//then
		Thread.sleep(1000);
		Product product = productRepository.findById(1L).orElseThrow();
		Assertions.assertThat(product.getItemsAvailable()).isEqualTo(95);
		Assertions.assertThat(product.getItemsReserved()).isEqualTo(5);
	}

	private static Order prepareOrder(Status status, boolean stockStarted, int productCount, long productId) {
		return Order.builder()
				.id(productId)
				.status(status)
				.productId(productId)
				.stockStarted(stockStarted)
				.productCount(productCount)
				.build();
	}
}
