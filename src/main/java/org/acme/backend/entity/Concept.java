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
import static com.mongodb.client.model.Projections.include;

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
        return mongoCollection().aggregate(pipeline, Document.class)
                .collect().first()
                .map(document -> document == null
                        ? 0L
                        : document.get("total", Number.class).longValue());
    }

    public static Multi<String> getAllTopicName() {
        return mongoCollection().distinct("topic.name", String.class);
    }

    public static Multi<WordPairEntry> getWordPairsByTopicNameAndLangs(String topicName,
                                                                       String sourceLang,
                                                                       String targetLang) {
        if (StringUtils.isAllBlank(topicName, sourceLang, targetLang)) {
            return Multi.createFrom().empty();
        }

        String sourceLangLower = sourceLang.toLowerCase();
        String targetLangLower = targetLang.toLowerCase();

        List<Bson> pipeline = List.of(
                match(and(exists("topic"), eq("topic.name", topicName))),
                // Find index of source and target languages in words array
                project(new Document("sourceIdx", new Document("$indexOfArray", List.of("$words.language", sourceLangLower)))
                        .append("targetIdx", new Document("$indexOfArray", List.of("$words.language", targetLangLower)))
                        .append("words", "$words")),
                // Only include where both languages exist (index >= 0)
                match(and(gte("sourceIdx", 0), gte("targetIdx", 0))),
                // Extract words at found indices
                project(new Document("sourceWord", new Document("$arrayElemAt", List.of("$words.text", "$sourceIdx")))
                        .append("sourceWordDescription", new Document("$arrayElemAt", List.of("$words.comment", "$sourceIdx")))
                        .append("targetWord", new Document("$arrayElemAt", List.of("$words.text", "$targetIdx")))
                        .append("targetWordDescription", new Document("$arrayElemAt", List.of("$words.comment", "$targetIdx")))
                ));
        return mongoCollection().aggregate(pipeline, WordPairEntry.class);
    }
}
