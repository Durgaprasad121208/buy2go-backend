package com.buy2go.api.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class BaseEntity {

    @CreatedDate
    private Instant createdAt = Instant.now();

    @LastModifiedDate
    private Instant updatedAt = Instant.now();

    private boolean isDeleted = false;

    private Instant deletedAt;
}
