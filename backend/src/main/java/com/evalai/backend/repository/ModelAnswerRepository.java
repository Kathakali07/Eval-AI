package com.evalai.backend.repository;

import com.evalai.backend.model.ModelAnswer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ModelAnswerRepository extends MongoRepository<ModelAnswer, String> {

    List<ModelAnswer> findBySubject(String subject);

    List<ModelAnswer> findByQuestion(String question);

}
