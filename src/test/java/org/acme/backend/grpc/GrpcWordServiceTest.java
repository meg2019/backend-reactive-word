package org.acme.backend.grpc;

import com.google.protobuf.Empty;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import org.acme.backend.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class GrpcWordServiceTest {

    private static final List<String> ACTUAL_TOPIC_NAME = List.of("Глаголы");

    @GrpcClient
    WordService wordService;

    @Test
    @RunOnVertxContext
    @DisplayName("Should return topic count reactively")
    void shouldReturnTopicCountReactive(UniAsserter asserter) {

        asserter.assertEquals(
                () -> wordService.getTopicCount(Empty.getDefaultInstance())
                        .map(TopicCount::getCount)
                        .ifNoItem()
                        .after(Duration.ofSeconds(5L))
                        .failWith(new AssertionError("No topic count received within 5 seconds")),

                1L
        );
    }

    @Test
    @RunOnVertxContext
    @DisplayName("Should return all unique topics reactively")
    void shouldReturnAllUniqueTopicsReactive(UniAsserter asserter) {

        asserter.assertThat(
                () -> wordService.getTopics(Empty.getDefaultInstance())
                        .collect().asList()
                        .ifNoItem()
                        .after(Duration.ofSeconds(5L))
                        .failWith(new AssertionError("No topics name received within 5 seconds")),

                list -> {
                    assertNotNull(list);
                    assertEquals(1, list.size());
                    assertTrue(list.stream().map(Topic::getName).allMatch(ACTUAL_TOPIC_NAME::contains));
                });
    }

    @Test
    @RunOnVertxContext
    @DisplayName("Should return word pairs for first topic reactively")
    void shouldReturnFirstTopicWordPairsReactive(UniAsserter asserter) {

        WordPairRequest request = WordPairRequest.newBuilder()
                .setTopicName("Глаголы")
                .setSourceLanguage("HE")
                .setTargetLanguage("RU")
                .build();

        asserter.assertThat(
                () -> wordService.getWordPairs(request)
                        .collect().asList()
                        .ifNoItem()
                        .after(Duration.ofSeconds(5L))
                        .failWith(new AssertionError("No word pairs received within 5 seconds")),

                list -> {
                    assertNotNull(list);
                    assertEquals(100, list.size());
                    list.stream().limit(5).forEach(wp ->
                            Log.infof(
                                    "source_word: %s, source_word_desc: %s, target_word: %s, target_word_desc: %s%n"
                                            .formatted(wp.getSourceWord(), wp.getSourceWordDesc(),
                                                    wp.getTargetWord(), wp.getTargetWordDesc())));
                });
    }
}