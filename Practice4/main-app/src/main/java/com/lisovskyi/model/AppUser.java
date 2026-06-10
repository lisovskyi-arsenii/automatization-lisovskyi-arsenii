package com.lisovskyi.model;

import com.lisovskyi.annotations.ColumnMapping;
import com.lisovskyi.annotations.DatabaseEntity;
import com.lisovskyi.annotations.RuntimeValidate;
import lombok.*;

@Setter @Getter
@NoArgsConstructor @AllArgsConstructor
@DatabaseEntity
@ToString
public class AppUser {
    private Long id;
    @RuntimeValidate
    private String username;
    @ColumnMapping("user_email")
    private String email;
    private String role;
}
