package com.example.cellex.repositories.product;

import com.example.cellex.models.mongo.LivestreamCommentDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface LivestreamCommentRepository extends MongoRepository<LivestreamCommentDocument, String> {
    List<LivestreamCommentDocument> findBySessionIdOrderByCreatedAtAsc(String sessionId);
}