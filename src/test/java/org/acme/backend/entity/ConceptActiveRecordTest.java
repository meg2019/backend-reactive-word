package org.acme.backend.entity;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class ConceptActiveRecordTest {


    private static final String TOPIC_NAME_1 = "Глаголы";
    private static final String TOPIC_NAME_2 = "UnExisting";
    private static final String HE_LANG = "HE";
    private static final String RU_LANG = "RU";


    private static Stream<Arguments> findWordsByTopicNameTestCases() {
        return Stream.of(
                Arguments.of(TOPIC_NAME_1, 200L),
                Arguments.of(TOPIC_NAME_2, 0),
                Arguments.of(null, 0),
                Arguments.of("", 0),
                Arguments.of(" ", 0)
        );
    }


    @Test
    @RunOnVertxContext
    @DisplayName("Should return correct count of existing concepts")
    void shouldReturnConceptsCount(UniAsserter asserter) {
        asserter.assertEquals(
                Concept::getAllConceptsCount,
                100L
        );
    }

    @Test
    @RunOnVertxContext
    @DisplayName("Should return concepts by topic name")
    void shouldReturnConceptsByTOPIC_NAME_1(UniAsserter asserter) {
        asserter.assertThat(
                () -> Concept.findAllConceptsByTopicName(TOPIC_NAME_1)
                        .collect()
                        .asList(),
                conceptsList -> assertEquals(
                        100L,
                        conceptsList.size(),
                        "We got %d concepts for topicName: '%s'".formatted(conceptsList.size(), TOPIC_NAME_1))
        );
    }


    @Test
    @RunOnVertxContext
    @DisplayName("Should return concepts by topic name")
    void shouldReturnConceptsByTOPIC_NAME_3(UniAsserter asserter) {
        asserter.assertThat(
                () -> Concept.findAllConceptsByTopicName(TOPIC_NAME_2)
                        .collect()
                        .asList(),
                conceptsList -> assertEquals(
                        0L,
                        conceptsList.size(),
                        "We got %d concepts for topicName: '%s'".formatted(conceptsList.size(), TOPIC_NAME_2))
        );
    }

    @Test
    @RunOnVertxContext
    @DisplayName("Should return concepts by topic name")
    void shouldReturnConceptsByNull(UniAsserter asserter) {
        asserter.assertThat(
                () -> Concept.findAllConceptsByTopicName(null)
                        .collect()
                        .asList(),
                conceptsList -> assertEquals(
                        0L,
                        conceptsList.size(),
                        "We got %d concepts for topicName: '%s'".formatted(conceptsList.size(), null))
        );
    }

    @ParameterizedTest
    @MethodSource("findWordsByTopicNameTestCases")
    @DisplayName("Should return words by topic name")
    @SneakyThrows
    void shouldReturnWordsByTopicName(String topicName, long expectedWordCount) {
        CompletableFuture<List<Concept.Word>> addFuture = new CompletableFuture<>();
        Concept.findWordsByTopicName(topicName)
                .collect()
                .asList()
                .subscribe().with(
                        addFuture::complete,
                        addFuture::completeExceptionally
                );
        List<Concept.Word> actualConceptsList = addFuture.get(10, TimeUnit.SECONDS);

        assertEquals(expectedWordCount, actualConceptsList.size());
    }

    //    @Test
//    @DisplayName("Should return a correct count of unique topics")
//    @SneakyThrows
//    void shouldReturnUniqueTopicsCount() {
//        CompletableFuture<Long> countFuture = new CompletableFuture<>();
//        Concept.getAllTopicsCounts()
//                .subscribe().with(
//                        countFuture::complete,
//                        countFuture::completeExceptionally
//                );
//        Long actualUniqueTopicsCount = countFuture.get(10, TimeUnit.SECONDS);
//
//        assertEquals(2L, actualUniqueTopicsCount);
//    }
    @Test
    @RunOnVertxContext
    @DisplayName("Should return correct count of unique topics")
    void shouldReturnUniqueTopicsCount(UniAsserter asserter) {
        asserter.assertEquals(
                Concept::getTopicsCounts,
                1L
        );
    }

    @Test
    @RunOnVertxContext
    @DisplayName("Should return all unique topic names")
    void shouldReturnUniqueTopicsName(UniAsserter asserter) {
        asserter.assertThat(
                () -> Concept.getAllTopicName()
                        .collect()
                        .asList(),
                actualResultList -> {
                    assertNotNull(actualResultList);
                    assertEquals(1, actualResultList.size());
                    assertEquals(Set.of(TOPIC_NAME_1), new HashSet<>(actualResultList));
                });
    }

//    @Test
//    @DisplayName("Should return a unique topics name")
//    @SneakyThrows
//    void shouldReturnUniqueTopicsName() {
//        CompletableFuture<List<String>> countFuture = new CompletableFuture<>();
//        Concept.getAllTopicName()
//                .collect().asList()
//                .subscribe().with(
//                        countFuture::complete,
//                        countFuture::completeExceptionally
//                );
//        List<String> actualTopicNames = countFuture.get(10, TimeUnit.SECONDS);
//
//        assertEquals(2, actualTopicNames.size());
//        assertTrue(actualTopicNames.containsAll(Set.of(TOPIC_NAME_1, TOPIC_NAME_2)));
//    }

    @Test
    @DisplayName("Should return word pairs by topic name")
    @RunOnVertxContext
    void shouldReturnWordPairsByTopicName(UniAsserter asserter) {

        asserter.assertThat(
                () -> Concept.getWordPairsByTopicNameAndLangs(TOPIC_NAME_1, HE_LANG, RU_LANG)
                        .collect()
                        .asSet(),
                r -> {
                    assertNotNull(r);
                    assertEquals(100L, r.size());
                });

        asserter.assertThat(
                () -> Concept.getWordPairsByTopicNameAndLangs(TOPIC_NAME_2, HE_LANG, RU_LANG)
                        .collect()
                        .asSet(),
                r -> {
                    assertNotNull(r);
                    assertTrue(r.isEmpty());
                });


        asserter.assertThat(
                () -> Concept.getWordPairsByTopicNameAndLangs(TOPIC_NAME_1, "FR", RU_LANG)
                        .collect()
                        .asSet(),
                r -> {
                    assertNotNull(r);
                    assertTrue(r.isEmpty());
                });

        asserter.assertThat(
                () -> Concept.getWordPairsByTopicNameAndLangs(null, HE_LANG, RU_LANG)
                        .collect()
                        .asSet(),
                r -> {
                    assertNotNull(r);
                    assertTrue(r.isEmpty());
                });

//        CompletableFuture<List<Map<String, String>>> addFuture = new CompletableFuture<>();
//        Concept.getHeRuPairsByTopicName(topicName)
//                .collect().asList()
//                .subscribe().with(
//                        addFuture::complete,
//                        addFuture::completeExceptionally
//                );
//        List<Map.Entry<String, String>> actualResult = addFuture.get(10, TimeUnit.SECONDS).stream()
//                .flatMap(el -> el.entrySet().stream()).toList();
//
//        assertEquals(expectedWordPairs.size(), actualResult.size());
//
//        List<Map.Entry<String, String>> expectedResult = expectedWordPairs.entrySet().stream().toList();
//
//        assertTrue(actualResult.containsAll(expectedResult));
    }
}