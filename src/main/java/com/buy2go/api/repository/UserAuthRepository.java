package com.buy2go.api.repository;

import com.buy2go.api.document.UserAuthDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAuthRepository extends MongoRepository<UserAuthDocument, String> {
    Optional<UserAuthDocument> findByUserNameAndIsDeletedFalse(String userName);
    Optional<UserAuthDocument> findByUserIdAndIsDeletedFalse(String userId);
    boolean existsByUserName(String userName);
}
