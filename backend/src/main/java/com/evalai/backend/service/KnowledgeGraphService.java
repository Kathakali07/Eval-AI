package com.evalai.backend.service;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class KnowledgeGraphService {

        private final Neo4jClient neo4jClient;

        public KnowledgeGraphService(Neo4jClient neo4jClient) {
                this.neo4jClient = neo4jClient;
        }

        /**
         * Inserts a subject-predicate-object triplet into the Neo4j knowledge graph.
         */
        public void insertTriplet(String subject, String predicate, String object, String subjectArea) {
                String safePredicate = predicate.toUpperCase().replaceAll("[^A-Z0-9]", "_");

                String cypherQuery = String.format(
                                "MERGE (sub:Concept {name: $subName}) " +
                                                "SET sub.subject_area = $subjectArea " +
                                                "MERGE (obj:Concept {name: $objName}) " +
                                                "SET obj.subject_area = $subjectArea " +
                                                "MERGE (sub)-[r:%s]->(obj)",
                                safePredicate);

                neo4jClient.query(cypherQuery)
                                .bindAll(Map.of(
                                                "subName", subject.toLowerCase(),
                                                "objName", object.toLowerCase(),
                                                "subjectArea", subjectArea))
                                .run();
        }
}