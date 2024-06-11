package com.jsbs.casemall.dto;


import com.jsbs.casemall.entity.Cart;
import com.jsbs.casemall.entity.CartItem;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class CartDto {
    private String userId;
    private Long cartId;

    // form 에서 받을 데이터
    private List<CartItemDto> items;
    private int totalPrice;

    // 디비에서  가져올때
    public CartDto(Cart cart) {
        this.cartId = cart.getId();
        this.userId = cart.getUser().getUserId();
        this.items = new ArrayList<>();

        for (CartItem cartItem : cart.getCartItems()) {
            this.items.add(new CartItemDto(cartItem));
        }
    }
}
