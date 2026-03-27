package org.acme.backend.grpc;

import com.google.protobuf.Empty;
import io.quarkus.grpc.GrpcService;
import io.quarkus.runtime.util.StringUtil;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.acme.backend.entity.Concept;
import org.acme.backend.model.*;

@GrpcService
public class GrpcWordService implements WordService {

    @Override
    public Uni<TopicCount> getTopicCount(Empty request) {
        return Concept.getTopicsCounts()
                .map(count -> TopicCount.newBuilder()
                        .setCount(count)
                        .build());
    }

    @Override
    public Multi<Topic> getTopics(Empty request) {
        return Concept.getAllTopicName()
                .map(name -> Topic.newBuilder()
                        .setName(name)
                        .build());
    }

    @Override
    public Multi<WordPair> getWordPairs(WordPairRequest request) {
        return Concept.getWordPairsByTopicNameAndLangs(
                        request.getTopicName(),
                        request.getSourceLanguage(),
                        request.getTargetLanguage()
                )
                .map(pair -> WordPair.newBuilder()
                        .setSourceWord(pair.sourceWord())
                        .setSourceWordDesc(resultClearer(pair.sourceWordDescription()))
                        .setTargetWord(pair.targetWord())
                        .setTargetWordDesc(resultClearer(pair.targetWordDescription()))
                        .build());
    }

    private static String resultClearer(String result) {
        return StringUtil.isNullOrEmpty(result) ? "no-comment" : result.strip();
    }
}
