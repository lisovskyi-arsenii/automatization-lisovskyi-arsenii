package com.lisovskyi.model;

import com.lisovskyi.annotations.DatabaseEntity;
import com.lisovskyi.annotations.RuntimeValidate;
import lombok.*;

@Setter @Getter
@NoArgsConstructor @AllArgsConstructor
@DatabaseEntity
@ToString
public class Product {
    private Long id;
    @RuntimeValidate
    private String name;
    private Double price;
    private Integer stockQuantity;
}
