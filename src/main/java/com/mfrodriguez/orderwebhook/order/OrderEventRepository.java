package com.mfrodriguez.orderwebhook.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderEventRepository extends JpaRepository<OrderEvent, String> {

    List<OrderEvent> findByOrderIdOrderByReceivedAtAsc(String orderId);
}
