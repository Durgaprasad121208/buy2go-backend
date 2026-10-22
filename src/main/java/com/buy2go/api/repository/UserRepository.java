package com.buy2go.api.repository;

import com.buy2go.api.document.UserDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<UserDocument, String> {
    Optional<UserDocument> findByEmailAndIsDeletedFalse(String email);

    @Query("{ 'isDeleted': false, $or: [ { 'firstName': { $regex: ?0, $options: 'i' } }, { 'lastName': { $regex: ?0, $options: 'i' } }, { 'email': { $regex: ?0, $options: 'i' } } ] }")
    Page<UserDocument> searchUsers(String search, Pageable pageable);

    Page<UserDocument> findByIsDeletedFalse(Pageable pageable);
}
