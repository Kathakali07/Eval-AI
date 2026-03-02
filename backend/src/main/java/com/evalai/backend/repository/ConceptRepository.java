package com.evalai.backend.repository;

import com.evalai.backend.model.ConceptNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface ConceptRepository extends Neo4jRepository<ConceptNode, String> {

    @Query("MATCH (sub:Concept {subject_area: $subjectArea})-[rel]->(obj:Concept) " +
            "WHERE sub.name IN $coreConcepts " +
            "RETURN sub.name AS subject, type(rel) AS predicate, obj.name AS object")
    List<Map<String, Object>> getExpandedKnowledgeGraph(
            @Param("subjectArea") String subjectArea,
            @Param("coreConcepts") List<String> coreConcepts
    );
}
