package com.buy2go.api.repository;

import com.buy2go.api.document.RoleDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends MongoRepository<RoleDocument, String> {
    Optional<RoleDocument> findByName(String name);
}
