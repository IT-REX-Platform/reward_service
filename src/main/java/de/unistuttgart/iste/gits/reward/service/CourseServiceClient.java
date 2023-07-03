package de.unistuttgart.iste.gits.reward.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class CourseServiceClient {

    @Value("${course_service.url}")
    private String courseServiceUrl;

    public List<UUID> getChapterIds(UUID courseId) {
        WebClient webClient = WebClient.builder().baseUrl(courseServiceUrl).build();

        GraphQlClient graphQlClient = HttpGraphQlClient.builder(webClient).build();

        String query = """
                query($courseId: UUID!) {
                    coursesById(ids: [$courseId]) {
                        chapters {
                            elements {
                                id
                            }
                        }
                    },
                }
                """;

        return Optional.ofNullable(graphQlClient.document(query)
                        .variable("courseId", courseId)
                        .retrieve("coursesById[0].chapters.elements")
                        .toEntityList(ChapterWithId.class)
                        .doOnError(e -> log.error("Error while retrieving chapter ids from course service", e))
                        .block())
                .map(list -> list.stream().map(ChapterWithId::id).toList())
                .orElse(List.of());

    }

    // helper class for deserialization
    record ChapterWithId(UUID id) {
    }

    public UUID getCourseIdForContent(UUID contentId) {
        WebClient webClient = WebClient.builder().baseUrl(courseServiceUrl).build();

        GraphQlClient graphQlClient = HttpGraphQlClient.builder(webClient).build();

        String query = """
                query($contentId: UUID!) {
                    resourceById(ids: [$contentId]) {
                        availableCourses
                    }
                }
                """;

        return Optional.ofNullable(graphQlClient.document(query)
                        .variable("contentId", contentId)
                        .retrieve("resourceById[0].availableCourses[0]")
                        .toEntity(UUID.class)
                        .doOnError(e -> log.error("Error while retrieving course id from course service", e))
                        .block())
                .orElseThrow();
    }
}
