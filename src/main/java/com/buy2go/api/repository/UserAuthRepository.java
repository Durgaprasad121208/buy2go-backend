package com.buy2go.api.repository;

import com.buy2go.api.entity.UserAuthEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAuthRepository extends MongoRepository<UserAuthEntity, String> {
    Optional<UserAuthEntity> findByEmailAndIsDeletedFalse(String email);
    Optional<UserAuthEntity> findByUserIdAndIsDeletedFalse(String userId);
    boolean existsByEmail(String email);
}
