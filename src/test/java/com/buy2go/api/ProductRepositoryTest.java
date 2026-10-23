
package com.buy2go.api;

import com.buy2go.api.entity.ProductEntity;
import com.buy2go.api.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    @Test
    void testFindById() {
        System.out.println("ALL PRODUCTS COUNT: " + productRepository.count());
        productRepository.findAll().forEach(p -> System.out.println("FOUND PRODUCT: id=" + p.getId() + ", title=" + p.getTitle() + ", isDeleted=" + p.isDeleted()));
        
        Optional<ProductEntity> byId = productRepository.findById("6adb1685ff5f8909e6b50063");
        System.out.println("findById result: " + byId);

        org.bson.Document rawDoc = mongoTemplate.getCollection("products").find().first();
        System.out.println("RAW BSON DOC: " + (rawDoc != null ? rawDoc.toJson() : "null"));
        if (rawDoc != null) {
            Object idVal = rawDoc.get("_id");
            System.out.println("_id class: " + (idVal != null ? idVal.getClass().getName() : "null"));
            System.out.println("_id value: " + idVal);
            System.out.println("_id equals string '6adb1685ff5f8909e6b50063': " + "6adb1685ff5f8909e6b50063".equals(String.valueOf(idVal)));
        }

        org.springframework.data.mongodb.core.query.Query qDoc = new org.springframework.data.mongodb.core.query.Query(
            org.springframework.data.mongodb.core.query.Criteria.where("_id").is(new org.bson.types.ObjectId("6adb1685ff5f8909e6b50063"))
        );
        System.out.println("findOne as Document: " + mongoTemplate.findOne(qDoc, org.bson.Document.class, "products"));
        org.springframework.data.mongodb.core.convert.QueryMapper queryMapper = new org.springframework.data.mongodb.core.convert.QueryMapper(mongoTemplate.getConverter());
        org.bson.Document mapped = queryMapper.getMappedObject(qDoc.getQueryObject(), mongoTemplate.getConverter().getMappingContext().getPersistentEntity(ProductEntity.class));
        System.out.println("Mapped query for ProductEntity: " + mapped.toJson());
        System.out.println("Mapped query _id value type: " + mapped.get("_id").getClass().getName());
    }
}
