package com.lisovskyi.model;

import com.lisovskyi.annotations.DatabaseEntity;
import lombok.*;

@Setter @Getter
@NoArgsConstructor @AllArgsConstructor
@DatabaseEntity
@ToString
public class Order {
    private Long id;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private Double totalAmount;
}
