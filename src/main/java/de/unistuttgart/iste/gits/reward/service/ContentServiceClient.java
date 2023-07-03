package de.unistuttgart.iste.gits.reward.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class ContentServiceClient {

    @Value("${content_service.url}")
    private String contentServiceUrl;

    public List<ContentWithUserProgressData> getContentsWithUserProgressData(UUID userId,
                                                                             List<UUID> chapterIds) {
        WebClient webClient = WebClient.builder().baseUrl(contentServiceUrl).build();

        GraphQlClient graphQlClient = HttpGraphQlClient.builder(webClient).build();

        String query = """
                query($userId: UUID!, $chapterIds: [UUID!]!) {
                    contentsByChapterIds(chapterIds: $chapterIds) {
                        id
                        metadata {
                            name
                            tagNames
                            suggestedDate
                            type
                            chapterId
                            rewardPoints
                        }
                        userProgressData(userId: $userId) {
                            userId
                            contentId
                            learningInterval
                            nextLearnDate
                            lastLearnDate
                            log {
                                timestamp
                                success
                                correctness
                                hintsUsed
                                timeToComplete
                            }
                        }
                    }
                }
                                
                """;

        List<ContentWithUserProgressData[]> result = graphQlClient.document(query)
                .variable("userId", userId)
                .variable("chapterIds", chapterIds)
                .retrieve("contentsByChapterIds")
                .toEntityList(ContentWithUserProgressData[].class)
                .block();

        if (result == null) {
            return List.of();
        }

        return result.stream()
                .flatMap(Arrays::stream)
                .toList();
    }
}
