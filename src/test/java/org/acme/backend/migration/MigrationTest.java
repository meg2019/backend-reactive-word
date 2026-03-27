package org.acme.backend.migration;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class MigrationTest {

    @Inject
    MongoClient mongoClient;


    private MongoCollection<Document> getCollection() {
        return mongoClient.getDatabase("words").getCollection("concepts");
    }

    private static Stream<Arguments> provideTestTopics() {
        return Stream.of(
                Arguments.of("Глаголы", 100L)
        );
    }

    public static Stream<Arguments> provideTestWords() {
        return Stream.of(
                Arguments.of("Глаголы", "RU", 100L),
                Arguments.of("Глаголы", "HE", 100L)
        );
    }

    @Test
    @DisplayName("Should migrate all collection")
    void shouldMigrateAllCollection() {

        long actualDocumentCount = getCollection().countDocuments();
        assertEquals(100L, actualDocumentCount);
    }

    @ParameterizedTest
    @MethodSource("provideTestTopics")
    @DisplayName("Should migrate all concepts for topicName")
    void shouldMigrateConceptsTopic(String topicName, Long expectedConceptsCount) {

        List<Bson> aggregation = List.of(
                match(eq("topic.name", topicName)),
                count("total")
        );
        Document result = getCollection().aggregate(aggregation, Document.class).first();
        assertAll("result should be not null and have expected value",
                () -> assertNotNull(result),
                () -> assertEquals(expectedConceptsCount, ((Number) result.get("total")).longValue())
        );
    }

    @ParameterizedTest
    @MethodSource("provideTestWords")
    @DisplayName("Should migrate all words for topic")
    void shouldMigrateWordsForTopic(String topicName, String lang, long expectedWordsCount) {

        List<Bson> aggregation = List.of(
                match(eq("topic.name", topicName)),
                unwind("$words"),
                match(eq("words.language", lang.toLowerCase())),
                project(fields(excludeId(), include("words.text")))
        );
        List<String> actualWordList = getWordListFromAggregation(aggregation);
        assertAll("result should not be null and have expected value",
                () -> assertNotNull(actualWordList),
                () -> assertEquals(expectedWordsCount, actualWordList.size())
        );

    }

    private  List<String> getWordListFromAggregation(List<Bson> pipeline) {
        return StreamSupport.stream(getCollection().aggregate(pipeline, Document.class).spliterator(), false)
                .flatMap(doc -> Optional.ofNullable(doc.getEmbedded(List.of("words", "text"), String.class))
                        .stream()).toList();
    }
}
