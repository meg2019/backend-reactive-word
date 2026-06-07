package org.acme.backend.entity;


import com.mongodb.client.model.Aggregates;
import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.conversions.Bson;

import java.util.*;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;

@MongoEntity(collection = "concepts")
public class Concept extends ReactivePanacheMongoEntity {

    public Topic topic;
    public List<Word> words;
    public String comment;

    public record Topic(
            String name,
            String description) {
    }

    public record Word(
            /**
             * ISO 639-1 language code (2-letter code) for the word's language.
             * Using standardized language codes ensures consistency and
             * enables proper internationalization support.
             * <p>
             * Examples:
             * - "en" for English
             * - "he" for Hebrew
             * - "ru" for Russian
             * - "es" for Spanish
             * - "fr" for French
             */
            String language,
            String text,
            /**
             * Part of speech classification for the word.
             * Helps users understand grammatical usage and context.
             * <p>
             * Common values:
             * - "noun" - person, place, thing, or idea
             * - "verb" - action or state of being
             * - "adjective" - describes or modifies nouns
             * - "adverb" - modifies verbs, adjectives, or other adverbs
             * - "preposition" - shows relationships between words
             * - "conjunction" - connects words or phrases
             * - "interjection" - expresses emotion
             */
            @BsonProperty("speech-part")
            String partOfSpeech,
            String comment,
            /**
             * URL to an audio file containing the pronunciation of the word.
             * Enables users to hear correct pronunciation, which is especially
             * valuable for language learning and unfamiliar scripts.
             * <p>
             * Example: "https://audio.example.com/pronunciations/house_en.mp3"
             */
            @BsonProperty("url-audio")
            String audioUrl
    ) {
    }


//    @ProjectionFor(Concept.class)
//    public class WordsProjection {
//        public List<Word> words;
//    }
//
//    @ProjectionFor(Concept.class)
//    public class TopicProjection {
//        public Topic topic;
//    }

    public record WordPairEntry(
            String sourceWord,
            String sourceWordDescription,
            String targetWord,
            String targetWordDescription) {
    }

    public record TopicsCountResult(long total) {}

    public static Uni<Long> getAllConceptsCount() {
        return count();
    }

    public static Multi<Concept> findAllConceptsByTopicName(String topicName) {
        if (StringUtils.isBlank(topicName)) {
            return Multi.createFrom().empty();
        }
        return stream("topic.name", topicName);
    }

    public static Multi<Word> findWordsByTopicName(String topicName) {
//        return find("topic.name", topicName).project(WordsProjection.class)
//                .stream()
//                .flatMap(wordsProjection -> Multi.createFrom().iterable(wordsProjection.words));
        List<Bson> pipeline = List.of(
                match(and(exists("topic"), eq("topic.name", topicName))),
                unwind("$words"),           // Server-side array flattening
                replaceRoot("$words")       // Promote word to root
        );
        return mongoCollection().aggregate(pipeline, Word.class);
    }

//    public static Uni<Long> getAllTopicsCounts() {

    /// /        return find(Filters.exists("topic")).project(TopicProjection.class)
    /// /                .stream()
    /// /                .select()
    /// /                .distinct()
    /// /                .collect().with(Collectors.counting());
//        return Concept.getAllTopicName().collect().with(Collectors.counting());
//    }
    public static Uni<Long> getTopicsCounts() {
        List<Bson> pipeline = List.of(
                match(exists("topic")),
                group("$topic.name"),
                Aggregates.count("total")
        );
        return mongoCollection().aggregate(pipeline, TopicsCountResult.class)
                .collect().first()
                .map(result -> result == null ? 0 : result.total);
    }

    public static Multi<String> getAllTopicName() {
        return mongoCollection().distinct("topic.name", String.class);
    }

    public static Multi<WordPairEntry> getWordPairsByTopicNameAndLangs(String topicName,
                                                                       String sourceLang,
                                                                       String targetLang) {
        if (StringUtils.isAnyBlank(topicName, sourceLang, targetLang)) {
            return Multi.createFrom().empty();
        }

        String sourceLangLower = sourceLang.toLowerCase();
        String targetLangLower = targetLang.toLowerCase();

        List<Bson> pipeline = List.of(
                // ── Stage 1: Filter by topic name ──
                // Uses idx_concepts_topic_name index for O(log n) lookup
                // MongoDB: { $match: { topic: { $exists: true }, "topic.name": "Глаголы" } }
                match(and(exists("topic"), eq("topic.name", topicName))),

                // ── Stage 2: Find array positions of source & target languages ──
                // $indexOfArray returns 0-based index of first match, or -1 if not found
                // MongoDB: { $project: {
                //     sourceIdx: { $indexOfArray: ["$words.language", "he"] },
                //     targetIdx: { $indexOfArray: ["$words.language", "ru"] },
                //     words: 1
                // } }
                project(fields(
                        computed("sourceIdx",
                                new Document("$indexOfArray",
                                        List.of("$words.language", sourceLangLower))),
                        computed("targetIdx",
                                new Document("$indexOfArray",
                                        List.of("$words.language", targetLangLower))),
                        include("words")
                )),

                // ── Stage 3: Eliminate docs missing either language ──
                // If indexOfArray returned -1, this document is filtered out
                // MongoDB: { $match: { sourceIdx: { $gte: 0 }, targetIdx: { $gte: 0 } }
                match(and(gte("sourceIdx", 0), gte("targetIdx", 0))),

                // ── Stage 4: Extract words at found positions, drop _id ──
                // $arrayElemAt pulls the array element at the computed index
                // MongoDB: { $project: {
                //     sourceWord:            { $arrayElemAt: ["$words.text",    "$sourceIdx"] },
                //     sourceWordDescription: { $arrayElemAt: ["$words.comment", "$sourceIdx"] },
                //     targetWord:            { $arrayElemAt: ["$words.text",    "$targetIdx"] },
                //     targetWordDescription: { $arrayElemAt: ["$words.comment", "$targetIdx"] },
                //     _id: 0
                // } }
                project(fields(
                        computed("sourceWord",
                                new Document("$arrayElemAt",
                                        List.of("$words.text", "$sourceIdx"))),
                        computed("sourceWordDescription",
                                new Document("$arrayElemAt",
                                        List.of("$words.comment", "$sourceIdx"))),
                        computed("targetWord",
                                new Document("$arrayElemAt",
                                        List.of("$words.text", "$targetIdx"))),
                        computed("targetWordDescription",
                                new Document("$arrayElemAt",
                                        List.of("$words.comment", "$targetIdx"))),
                        excludeId()
                ))
        );
        return mongoCollection().aggregate(pipeline, WordPairEntry.class);
    }
}
