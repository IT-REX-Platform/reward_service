package de.unistuttgart.iste.gits.reward.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.*;
import de.unistuttgart.iste.gits.reward.persistence.dao.AllRewardScoresEntity;
import de.unistuttgart.iste.gits.reward.persistence.dao.RewardScoreEntity;
import de.unistuttgart.iste.gits.reward.persistence.dao.RewardScoreLogEntry;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * Test class for {@link PowerScoreCalculator}
 */
public class PowerScoreCalculatorTest {

    private final PowerScoreCalculator powerScoreCalculator = new PowerScoreCalculator();

    /**
     * Given a content exists
     * When calculateOnContentWorkedOn is called
     * Then the growth score is calculated depending on the other scores
     */
    @Test
    void calculateOnContentWorkedOnPowerScore() {
        UUID contentId = UUID.randomUUID();
        List<Content> contents = List.of(
                createContentWithUserData(contentId, UserProgressData.builder()
                        .setNextLearnDate(OffsetDateTime.now())
                        .setLog(List.of(
                                ProgressLogItem.builder()
                                        .setTimestamp(OffsetDateTime.now())
                                        .setCorrectness(1)
                                        .setSuccess(true)
                                        .setHintsUsed(0)
                                        .build()
                        ))
                        .build())
        );

        UserProgressLogEvent event = UserProgressLogEvent.builder()
                .userId(UUID.randomUUID())
                .contentId(contentId)
                .correctness(1)
                .hintsUsed(0)
                .success(true)
                .build();
        AllRewardScoresEntity rewardScoresEntity = createAllRewardScoresEntityWithPower(0);


        RewardScoreEntity power = powerScoreCalculator.calculateOnContentWorkedOn(rewardScoresEntity, contents,event);

        assertThat(power.getValue(), is(420));
    }

    /**
     * Given Fitness decreases to 0
     * When recalculateScore is called
     * Then the power score is decreased by the correct value and a log entry is added
     */
    @Test
    void powerDecreasesWhenFitnessDecreases() {
        UUID contentId = UUID.randomUUID();
        List<Content> contents = List.of(
                createContentWithUserData(contentId, UserProgressData.builder()
                        .setNextLearnDate(OffsetDateTime.now())
                        .setLog(List.of(
                                ProgressLogItem.builder()
                                        .setTimestamp(OffsetDateTime.now())
                                        .setCorrectness(1)
                                        .setSuccess(true)
                                        .setHintsUsed(0)
                                        .build()
                        ))
                        .build())
        );
        AllRewardScoresEntity rewardScoresEntity = createAllRewardScoresEntityWithPower(420);
        rewardScoresEntity.setFitness(initializeRewardScoreEntity());

        RewardScoreEntity power = powerScoreCalculator.recalculateScore(rewardScoresEntity, contents);


        assertThat(power.getValue(), is(220));
        assertThat(power.getLog(), hasSize(1));

        RewardScoreLogEntry logEntry = power.getLog().get(0);
        assertThat(logEntry.getDifference(), is(-200));
        assertThat(logEntry.getOldValue(), is(420));
        assertThat(logEntry.getNewValue(), is(220));
        assertThat(logEntry.getReason(), is(RewardChangeReason.COMPOSITE_VALUE));
    }





    private Content createContentWithUserData(UserProgressData userProgressData) {
        return createContentWithUserData(UUID.randomUUID(), userProgressData);
    }

    private Content createContentWithUserData(UUID contentId, UserProgressData userProgressData) {
        return FlashcardSetAssessment.builder()
                .setId(contentId)
                .setMetadata(ContentMetadata.builder().build())
                .setAssessmentMetadata(AssessmentMetadata.builder().build())
                .setUserProgressData(userProgressData)
                .build();
    }

    private AllRewardScoresEntity createAllRewardScoresEntityWithPower(int power) {
        return AllRewardScoresEntity.builder()
                .health(RewardScoreEntity.builder().value(100).build())
                .fitness(RewardScoreEntity.builder().value(100).build())
                .growth(RewardScoreEntity.builder().value(10).build())
                .strength(RewardScoreEntity.builder().value(10).build())
                .power(RewardScoreEntity.builder().value(power).build())
                .build();
    }

    private static RewardScoreEntity initializeRewardScoreEntity() {
        RewardScoreEntity rewardScoreEntity = new RewardScoreEntity();
        rewardScoreEntity.setValue(0);
        rewardScoreEntity.setLog(new ArrayList<>());
        return rewardScoreEntity;
    }
}
