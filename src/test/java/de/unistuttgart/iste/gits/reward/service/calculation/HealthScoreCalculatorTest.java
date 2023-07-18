package de.unistuttgart.iste.gits.reward.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.*;
import de.unistuttgart.iste.gits.reward.persistence.dao.AllRewardScoresEntity;
import de.unistuttgart.iste.gits.reward.persistence.dao.RewardScoreEntity;
import de.unistuttgart.iste.gits.reward.persistence.dao.RewardScoreLogEntry;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class HealthScoreCalculatorTest {

    private final HealthScoreCalculator healthScoreCalculator = new HealthScoreCalculator();

    /**
     * Given a content due for repetition exist with one day due
     * When recalculateScore is called
     * Then the health score is decreased by the correct value and a log entry is added
     */
    @Test
    void testRecalculateScores() {
        AllRewardScoresEntity rewardScoresEntity = createAllRewardScoresEntityWithHealthOf(100);
        UUID contentId = UUID.randomUUID();
        UserProgressData userProgressData = UserProgressData.builder().build();
        List<Content> contents = List.of(
                createContentWithUserData(contentId, userProgressData, 1)
        );

        RewardScoreEntity health = healthScoreCalculator.recalculateScore(rewardScoresEntity, contents);

        assertThat(health.getValue(), is(99));
        assertThat(health.getLog(), hasSize(1));

        RewardScoreLogEntry logEntry = health.getLog().get(0);
        assertThat(logEntry.getDifference(), is(-1));
        assertThat(logEntry.getReason(), is(RewardChangeReason.CONTENT_DUE_FOR_LEARNING));
        assertThat(logEntry.getAssociatedContentIds(), contains(contentId));
        assertThat(logEntry.getOldValue(), is(100));
        assertThat(logEntry.getNewValue(), is(99));
    }

    /**
     * Given 5 contents exist that are 10 days due
     * When recalculateScore is called
     * Then the health score is decreased by the correct value and a log entry is added
     */
    @Test
    void testRecalculateScoreMaxHealthDecrease() {
        AllRewardScoresEntity allRewardScores = createAllRewardScoresEntityWithHealthOf(100);
        UUID contentId = UUID.randomUUID();
        List<Content> contents = Collections.nCopies(5,
                createContentWithUserData(contentId,
                        UserProgressData.builder().build(), 10)
        );

        RewardScoreEntity health = healthScoreCalculator.recalculateScore(allRewardScores, contents);

        assertThat(health.getValue(), is(80)); // Max health decrease 20%
        assertThat(health.getLog(), hasSize(1));

        RewardScoreLogEntry logEntry = health.getLog().get(0);
        assertThat(logEntry.getDifference(), is(-20));
        assertThat(logEntry.getReason(), is(RewardChangeReason.CONTENT_DUE_FOR_LEARNING));
        assertThat(logEntry.getAssociatedContentIds(), hasItem(contentId));
        assertThat(logEntry.getAssociatedContentIds(), hasSize(5));
        assertThat(logEntry.getOldValue(), is(100));
        assertThat(logEntry.getNewValue(), is(80));
    }

    /**
     * Given 2 contents exist that are 7 days due
     * When recalculateScore is called
     * Then the health score is decreased by the correct value and a log entry is added
     */
    @Test
    void testRecalculateScore() {
        AllRewardScoresEntity allRewardScores = createAllRewardScoresEntityWithHealthOf(100);
        UUID contentId = UUID.randomUUID();
        List<Content> contents = Collections.nCopies(2,
                createContentWithUserData(contentId,
                        UserProgressData.builder().build(), 7)
        );

        RewardScoreEntity health = healthScoreCalculator.recalculateScore(allRewardScores, contents);

        assertThat(health.getValue(), is(92)); //100 - 0.5 * 2 * 8
        assertThat(health.getLog(), hasSize(1));

        RewardScoreLogEntry logEntry = health.getLog().get(0);
        assertThat(logEntry.getDifference(), is(-8));
        assertThat(logEntry.getReason(), is(RewardChangeReason.CONTENT_DUE_FOR_LEARNING));
        assertThat(logEntry.getAssociatedContentIds(), hasItem(contentId));
        assertThat(logEntry.getAssociatedContentIds(), hasSize(2));
        assertThat(logEntry.getOldValue(), is(100));
        assertThat(logEntry.getNewValue(), is(92));
    }

    /**
     * Given 1 content is due for learning
     * When calculateOnContentWorkedOn is called
     * Then the health score is increased and a log entry is added
     */
    @Test
    void calculateOnContentWorkedOn() {
        AllRewardScoresEntity allRewardScores = createAllRewardScoresEntityWithHealthOf(50);
        UUID contentId = UUID.randomUUID();
        List<Content> contents = List.of(
                createContentWithUserData(contentId, UserProgressData.builder().build(), 1)
        );
        UserProgressLogEvent event = UserProgressLogEvent.builder()
                .userId(UUID.randomUUID())
                .contentId(contentId)
                .correctness(1)
                .hintsUsed(0)
                .success(true)
                .build();

        RewardScoreEntity health = healthScoreCalculator.calculateOnContentWorkedOn(allRewardScores, contents, event);

        // should be 100 due to no contents being due
        assertThat(health.getValue(), is(100));
        assertThat(health.getLog(), hasSize(1));

        RewardScoreLogEntry logItem = health.getLog().get(0);
        assertThat(logItem.getDifference(), is(50));
        assertThat(logItem.getReason(), is(RewardChangeReason.CONTENT_DONE));
        assertThat(logItem.getAssociatedContentIds(), contains(contentId));
        assertThat(logItem.getOldValue(), is(50));
        assertThat(logItem.getNewValue(), is(100));
    }



    /**
     * Given no contents exist
     * When recalculateScore is called
     * Then the health score is not changed and no log entry is added
     */
    @Test
    void testRecalculateScoreWithoutContent() {
        AllRewardScoresEntity allRewardScores = createAllRewardScoresEntityWithHealthOf(100);

        RewardScoreEntity health = healthScoreCalculator.recalculateScore(allRewardScores, List.of());

        assertThat(health.getValue(), is(100));
        assertThat(health.getLog(), is(empty()));
    }

    /**
     * Given no content is due for repetition
     * When recalculateScore is called
     * Then the health score is not changed and no log entry is added
     */
    @Test
    void testRecalculateScoresWithoutContentsDueForRepetition() {
        AllRewardScoresEntity allRewardScores = createAllRewardScoresEntityWithHealthOf(100);
        List<Content> contents = List.of(
                createContentWithUserData(
                        UserProgressData.builder()
                                .setNextLearnDate(OffsetDateTime.now().plusDays(1))
                                .setLog(logWithOneSuccessfulEntry())
                                .build()),
                createContentWithUserData(
                        UserProgressData.builder()
                                .setNextLearnDate(OffsetDateTime.now().plusDays(3))
                                .setLog(logWithOneSuccessfulEntry())
                                .build())
        );

        RewardScoreEntity health = healthScoreCalculator.recalculateScore(allRewardScores, contents);

        // should not change as no content is due for repetition
        assertThat(health.getValue(), is(100));
        assertThat(health.getLog(), is(empty()));
    }

    /**
     * Given a content without a repetition date
     * When recalculateScore is called
     * Then the health score is not changed and no log entry is added
     */
    @Test
    void testRecalculateScoresWithContentThatHasNoRepetitionDate() {
        AllRewardScoresEntity allRewardScores = createAllRewardScoresEntityWithHealthOf(100);
        List<Content> contents = List.of(
                createContentWithUserData(
                        UserProgressData.builder()
                                .setNextLearnDate(null) // no repetition date, e.g. media content
                                .setLog(logWithOneSuccessfulEntry())
                                .build())
        );

        RewardScoreEntity health = healthScoreCalculator.recalculateScore(allRewardScores, contents);

        // should not change as no content is due for repetition
        assertThat(health.getValue(), is(100));
        assertThat(health.getLog(), is(empty()));
    }

    /**
     * Given only contents that were not learned yet exist
     * When recalculateScore is called
     * Then the health score is not changed and no log entry is added
     * <p>
     * Note: those contents should only affect health, not health
     */
    @Test
    void testRecalculateScoresWithNoContentsThatWereNotLearnedYet() {
        AllRewardScoresEntity allRewardScores = createAllRewardScoresEntityWithHealthOf(100);
        List<Content> contents = List.of(
                createContentWithUserData(
                        UserProgressData.builder()
                                .setNextLearnDate(OffsetDateTime.now().minusDays(1))
                                .setLog(List.of()) // not learned yet
                                .build()),
                createContentWithUserData(
                        UserProgressData.builder()
                                .setNextLearnDate(OffsetDateTime.now().minusDays(3))
                                .setLog(List.of(ProgressLogItem.builder()
                                        .setTimestamp(OffsetDateTime.now().minusDays(3))
                                        .setCorrectness(0)
                                        .setSuccess(false)
                                        .setHintsUsed(0)
                                        .build())) // not learned successfully yet
                                .build())
        );

        RewardScoreEntity health = healthScoreCalculator.recalculateScore(allRewardScores, contents);

        // should not change as no content is due for repetition
        assertThat(health.getValue(), is(100));
        assertThat(health.getLog(), is(empty()));
    }


    private List<ProgressLogItem> logWithOneSuccessfulEntry() {
        return List.of(
                ProgressLogItem.builder()
                        .setTimestamp(OffsetDateTime.now().minusDays(1))
                        .setCorrectness(1)
                        .setSuccess(true)
                        .setHintsUsed(0)
                        .build()
        );
    }

    private Content createContentWithUserData(UserProgressData userProgressData) {
        return createContentWithUserData(UUID.randomUUID(), userProgressData, 0);
    }

    private Content createContentWithUserData(UUID contentId, UserProgressData userProgressData, int overdue) {
        return FlashcardSetAssessment.builder()
                .setId(contentId)
                .setMetadata(ContentMetadata.builder().setSuggestedDate(OffsetDateTime.now().minusDays(overdue)).build())
                .setAssessmentMetadata(AssessmentMetadata.builder().build())
                .setUserProgressData(userProgressData)
                .build();
    }

    private AllRewardScoresEntity createAllRewardScoresEntityWithHealthOf(int health) {
        return AllRewardScoresEntity.builder()
                .health(RewardScoreEntity.builder().value(health).build())
                .fitness(RewardScoreEntity.builder().value(100).build())
                .growth(RewardScoreEntity.builder().value(0).build())
                .strength(RewardScoreEntity.builder().value(0).build())
                .power(RewardScoreEntity.builder().value(0).build())
                .build();
    }

}
