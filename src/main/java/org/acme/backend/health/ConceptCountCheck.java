package org.acme.backend.health;

import io.smallrye.health.api.AsyncHealthCheck;
import io.smallrye.health.api.Wellness;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.backend.entity.Concept;
import org.eclipse.microprofile.health.HealthCheckResponse;

import java.time.Duration;

@Wellness
@ApplicationScoped
public class ConceptCountCheck implements AsyncHealthCheck {


    @Override
    public Uni<HealthCheckResponse> call() {

        return Concept.getAllConceptsCount()
                .ifNoItem()
                .after(Duration.ofSeconds(10L))
                .failWith(TimeoutException::new)
                .map(count -> HealthCheckResponse.builder()
                        .name("Concept count check")
                        .status(count > 0)
                        .withData("concept-count", count)
                        .build())
                .onFailure(TimeoutException.class).recoverWithItem(throwable ->
                        HealthCheckResponse.builder()
                                .name("Concept count check")
                                .down()
                                .withData("error", "Database connection timeout, got no response within 10 seconds")
                                .build())
                .onFailure().recoverWithItem(throwable ->
                        HealthCheckResponse.builder()
                                .name("Concept count check")
                                .down()
                                .withData("error", String.format("Database query failed: %s", throwable.getMessage()))
                                .build());
    }
}
