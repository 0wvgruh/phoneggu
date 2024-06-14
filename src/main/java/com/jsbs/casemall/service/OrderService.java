package com.jsbs.casemall.service;

import com.jsbs.casemall.constant.OrderStatus;
import com.jsbs.casemall.dto.OrderDto;
import com.jsbs.casemall.dto.OrderItemDto;
import com.jsbs.casemall.entity.*;
import com.jsbs.casemall.exception.OutOfStockException;
import com.jsbs.casemall.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final ProductModelRepository productModelRepository;

    // GET 요청 처리 (주문 정보 조회)
    @Transactional(readOnly = true)
    public OrderDto getOrder(String userId) {
        Users user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다"));

        OrderDto orderDto = getExistingOrderDto(user);
        if (orderDto != null) {
            return orderDto;
        } else {
            return OrderDto.builder().build(); // 빈 주문 정보 반환
        }
    }

    // 기존에 있는 오더 있는지 확인
    @Transactional(readOnly = true)
    public OrderDto getExistingOrderDto(Users user) {
        List<Order> existingOrders = findExistingOrders(user);


        if (!existingOrders.isEmpty()) {
            Order existingOrder = existingOrders.get(0);
            // 장바구니 항목과 기존 주문 항목을 비교하여 업데이트
            updateOrderFromCartItems(existingOrder, user);

            List<OrderItemDto> orderItemDtos = existingOrder.getOrderItems().stream()
                    .map(OrderItemDto::new)
                    .collect(Collectors.toList());

            return OrderDto.builder()
                    .orderNo(existingOrder.getId())
                    .totalPrice(existingOrder.getOrderItems().stream().mapToInt(OrderDetail::getTotalPrice).sum())
                    .items(orderItemDtos)
                    .userName(user.getName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .build();
        }
        return null; // 기존 주문이 없으면 null 반환
    }
    // 생성된 주문들 찾기
    private List<Order> findExistingOrders(Users user) {
        return orderRepository.findByUsersAndOrderStatus(user, OrderStatus.STAY);
    }


    // 주문 생성
    public OrderDto createOrder(String userId, List<Long> itemIds) {
        // 로그인한 유저 찾기
        Users user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("해당 유저를 찾을 수 없습니다"));

        // 기존에 생성된 주문이 있는지 확인
        OrderDto findExistingOrderDto = getExistingOrderDto(user);
        if (findExistingOrderDto != null) {
            return findExistingOrderDto;
        }
        // 새로운 주문 생성
        List<OrderDetail> orderDetails = new ArrayList<>();
        int totalAmount = 0;

        // 체크한 제품 찾아서 OrderDetail 저장
        for (Long cartItemId : itemIds) {
            CartItem cartItem = cartItemRepository.findById(cartItemId).orElseThrow(() -> new IllegalArgumentException("찾는 아이템이 없습니다"));
            Product product = cartItem.getProduct();
            ProductModel productModel = cartItem.getProductModel();
            OrderDetail orderDetail = OrderDetail.createOrderDetails(product, productModel, cartItem.getCount());
            orderDetails.add(orderDetail);
            totalAmount += orderDetail.getTotalPrice();
        }

        Order order = Order.createOrder(user, orderDetails);
        orderRepository.save(order);
        List<OrderItemDto> orderItemDtos = orderDetails.stream()
                .map(OrderItemDto::new)
                .collect(Collectors.toList());

        return OrderDto.builder()
                .orderNo(order.getId())
                .totalPrice(totalAmount)
                .items(orderItemDtos)
                .userName(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .build();
    }

    // 주문 항목을 장바구니 항목과 비교하여 업데이트

    public void updateOrderFromCartItems(Order order, Users user) {
        Cart cart = cartRepository.findByUser(user);
        List<CartItem> cartItems = cart.getCartItems();
        // 각 아이템들을 순회
        for (CartItem cartItem : cartItems) {
            OrderDetail orderDetail = order.getOrderItems().stream()
                    .filter(orderItem -> orderItem.getProduct().getId().equals(cartItem.getProduct().getId()) &&
                            orderItem.getProductModel().getId().equals(cartItem.getProductModel().getId()))
                    .findFirst()
                    .orElse(null);
            int changeCount;
            if (orderDetail != null) { // 비어있지 않는다면
                if (orderDetail.getCount() != cartItem.getCount()) { // 주문상세의 개수와 카트에 담겨진 숫자가 다르다면
                    changeCount = orderDetail.getCount() - cartItem.getCount();
                    log.info("기존주문개수 - 카트에개수 : {}",changeCount);
                    // 재고 업데이트
                    if(changeCount > 0){
                        orderDetail.getProductModel().addStock(changeCount);
                    }else{
                        orderDetail.getProductModel().removeStock((Math.abs(changeCount)));
                    }
                    orderDetail.setCount(cartItem.getCount());
                }
            }else {
                // 장바구니에는 있고 오더에는 없는경우
                OrderDetail newOrderDetail = OrderDetail.createOrderDetails(cartItem.getProduct(), cartItem.getProductModel(), cartItem.getCount());
                order.getOrderItems().add(newOrderDetail);
            }

            orderRepository.save(order);
        }
    }

    // 주문 항목 삭제
    public void removeOrder(Long cartItemId, String userId) {
        Users user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다"));
        CartItem cartItem = cartItemRepository.findById(cartItemId).orElseThrow(() -> new IllegalArgumentException("찾는 아이템이 없습니다"));

        // 해당 주문 항목을 찾기
        Order order = orderRepository.findByUsersAndOrderStatus(user, OrderStatus.STAY).stream().findFirst().orElse(null);
        if (order == null) {
            throw new IllegalArgumentException("주문을 찾을 수 없습니다.");
        }

        OrderDetail orderDetail = order.getOrderItems().stream()
                .filter(item -> item.getProduct().getId().equals(cartItem.getProduct().getId()) &&
                        item.getProductModel().getId().equals(cartItem.getProductModel().getId()))
                .findFirst()
                .orElse(null);

        if (orderDetail != null) {
            // 재고 롤백
            orderDetail.getProductModel().addStock(orderDetail.getCount());

            // 주문 항목 삭제
            order.getOrderItems().remove(orderDetail);

            // 주문 항목이 없으면 주문 삭제
            if (order.getOrderItems().isEmpty()) {
                orderRepository.delete(order);
            } else {
                orderRepository.save(order);
            }
        } else {
            throw new IllegalArgumentException("주문 항목을 찾을 수 없습니다.");
        }
    }


}