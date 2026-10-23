package com.buy2go.api.repository;

import com.buy2go.api.entity.OrderEntity;
import com.buy2go.api.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<OrderEntity, String> {

    Optional<OrderEntity> findByIdAndIsDeletedFalse(String id);
    Optional<OrderEntity> findByOrderNumberAndIsDeletedFalse(String orderNumber);
    Optional<OrderEntity> findByOrderNumberIgnoreCaseAndIsDeletedFalse(String orderNumber);

    Page<OrderEntity> findByIsDeletedFalse(Pageable pageable);
    Page<OrderEntity> findByStatusAndIsDeletedFalse(OrderStatus status, Pageable pageable);

    @Query("{ 'isDeleted': false, $or: [ { 'orderNumber': { $regex: ?0, $options: 'i' } }, { 'customerName': { $regex: ?0, $options: 'i' } }, { 'location': { $regex: ?0, $options: 'i' } } ] }")
    Page<OrderEntity> searchOrders(String search, Pageable pageable);

    @Query("{ 'isDeleted': false, 'status': ?0, $or: [ { 'orderNumber': { $regex: ?1, $options: 'i' } }, { 'customerName': { $regex: ?1, $options: 'i' } }, { 'location': { $regex: ?1, $options: 'i' } } ] }")
    Page<OrderEntity> searchOrdersWithStatus(OrderStatus status, String search, Pageable pageable);

    long countByStatusAndIsDeletedFalse(OrderStatus status);
    long countByIsDeletedFalse();

    List<OrderEntity> findTop5ByIsDeletedFalseOrderByCreatedAtDesc();

    List<OrderEntity> findByCreatedAtBetweenAndIsDeletedFalse(Instant start, Instant end);

    @Query("{ 'isDeleted': false, $or: [ { 'customer.email': ?0 }, { 'customer.phone': ?0 } ] }")
    List<OrderEntity> findByCustomerIdentifier(String identifier);

    @Query("{ 'isDeleted': false, 'customer.email': ?0 }")
    List<OrderEntity> findByCustomerEmail(String email);

    @Query("{ 'isDeleted': false, 'customer.phone': ?0 }")
    List<OrderEntity> findByCustomerPhone(String phone);
}
