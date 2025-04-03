package org.tobiaszpietryga.stock.repository;

import org.springframework.data.repository.CrudRepository;
import org.tobiaszpietryga.stock.doman.Product;

public interface CustomerRepository extends CrudRepository<Product, Long> {

}
