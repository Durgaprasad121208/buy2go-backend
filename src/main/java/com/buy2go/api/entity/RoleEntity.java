package com.buy2go.api.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "roles")
public class RoleEntity extends BaseEntity {

    @Id
    private String id;

    private String name;
    private String description;

    public RoleEntity(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
