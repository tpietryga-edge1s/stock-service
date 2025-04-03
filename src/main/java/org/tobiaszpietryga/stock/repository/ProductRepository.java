package org.tobiaszpietryga.stock.repository;

import org.springframework.data.repository.CrudRepository;
import org.tobiaszpietryga.stock.doman.Product;

public interface ProductRepository extends CrudRepository<Product, Long> {

}
