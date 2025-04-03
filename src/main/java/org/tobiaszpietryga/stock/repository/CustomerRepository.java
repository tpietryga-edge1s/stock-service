package org.tobiaszpietryga.stock.repository;

import org.springframework.data.repository.CrudRepository;
import org.tobiaszpietryga.stock.doman.Customer;

public interface CustomerRepository extends CrudRepository<Customer, Long> {

}
