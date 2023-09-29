package de.unistuttgart.iste.gits.reward.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.Content;
import de.unistuttgart.iste.gits.generated.dto.RewardChangeReason;
import de.unistuttgart.iste.gits.reward.persistence.entity.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class HealthScoreCalculator implements ScoreCalculator {

    /**
     * Multiplier for the number of days a content is overdue, valid for the daily health decrease.
     */
    public static final double HEALTH_MODIFIER_PER_DAY = 0.5;
    /**
     * The maximum health decrease per day.
     */
    public static final double HEALTH_DECREASE_CAP = 20.0;

    /**
     * The maximum health value.
     */
    public static final int HEALTH_MAX = 100;

    /**
     * The minimum health value.
     */
    public static final int HEALTH_MIN = 0;

    @Override
    public RewardScoreEntity recalculateScore(final AllRewardScoresEntity allRewardScores,
                                              final List<Content> contents) {
        final RewardScoreEntity healthEntity = allRewardScores.getHealth();
        final int oldScore = healthEntity.getValue();

        final OffsetDateTime today = OffsetDateTime.now();

        final List<Content> newDueContents = getDueContentsThatWereNeverWorked(contents, today);

        final int diff = calculateHealthDecrease(newDueContents, today);
        final int newScore = Math.max(HEALTH_MIN, oldScore - diff);

        if (newScore - oldScore == 0) {
            // no change in health score, so no log entry is created
            return healthEntity;
        }

        final RewardScoreLogEntry logEntry = createLogEntryOnRecalculation(today, newScore, oldScore, newDueContents);

        healthEntity.setValue(newScore);
        healthEntity.getLog().add(logEntry);

        return healthEntity;
    }

    @Override
    public RewardScoreEntity calculateOnContentWorkedOn(
            final AllRewardScoresEntity allRewardScoresEntity,
            final List<Content> contents,
            final UserProgressLogEvent event) {
        final RewardScoreEntity rewardScore = allRewardScoresEntity.getHealth();

        final int oldScore = rewardScore.getValue();
        final int diffToFull = HEALTH_MAX - oldScore;

        if (diffToFull == 0) {
            return rewardScore;
        }

        final OffsetDateTime today = OffsetDateTime.now();

        final List<Content> newDueContents = getDueContentsThatWereNeverWorked(contents, today);
        int numberOfNewDueContentsBefore = newDueContents.size();

        // in case that the content list does not contain the content of the event
        // which usually should not happen
        if (!doesListContainContentWithId(newDueContents, event.getContentId())) {
            numberOfNewDueContentsBefore++;
        }

        final int healthIncrease = diffToFull / numberOfNewDueContentsBefore;
        final int newValue = Math.min(oldScore + healthIncrease, HEALTH_MAX);

        final RewardScoreLogEntry logEntry = createLogEntryOnContentWorkedOn(today, oldScore, newValue, event.getContentId());

        rewardScore.setValue(newValue);
        rewardScore.getLog().add(logEntry);

        return rewardScore;
    }

    private static RewardScoreLogEntry createLogEntryOnContentWorkedOn(final OffsetDateTime today,
                                                                       final int oldScore,
                                                                       final int newScore,
                                                                       final UUID contentId) {
        return RewardScoreLogEntry.builder()
                .date(today)
                .difference(newScore - oldScore)
                .newValue(newScore)
                .oldValue(oldScore)
                .reason(RewardChangeReason.CONTENT_DONE)
                .associatedContentIds(List.of(contentId))
                .build();
    }


    /**
     * Calculates the initial health value for a new entity.
     *
     * @param contents the contents of the course
     * @return the initial health value
     */
    public int calculateInitialHealthValueForNewEntity(final List<Content> contents) {
        final OffsetDateTime today = OffsetDateTime.now();

        final List<Content> newDueContents = getDueContentsThatWereNeverWorked(contents, today);
        final int healthDecrease = calculateHealthDecrease(newDueContents, today);

        // Calculate initial health value based on overdue, never-worked-on contents
        final int initialHealthValue = HEALTH_MAX - healthDecrease;

        // Ensure the initial health value is within bounds
        return Math.max(HEALTH_MIN, Math.min(HEALTH_MAX, initialHealthValue));
    }

    private static RewardScoreLogEntry createLogEntryOnRecalculation(final OffsetDateTime today,
                                                                     final int newScore,
                                                                     final int oldScore,
                                                                     final List<Content> newDueContents) {
        return RewardScoreLogEntry.builder()
                .date(today)
                .difference(newScore - oldScore)
                .newValue(newScore)
                .oldValue(oldScore)
                .reason(RewardChangeReason.CONTENT_DUE_FOR_LEARNING)
                .associatedContentIds(getIds(newDueContents))
                .build();
    }

    private static boolean doesListContainContentWithId(final List<Content> newDueContents, final UUID contentId) {
        return newDueContents.stream()
                .anyMatch(content -> content.getId().equals(contentId));
    }

    private static List<UUID> getIds(final List<Content> newDueContents) {
        return newDueContents.stream()
                .map(Content::getId)
                .toList();
    }

    /**
     * Calculates the health decrease based on the number of days the content is overdue.
     * The decrease is capped at {@link #HEALTH_DECREASE_CAP}.
     *
     * @param newDueContents the contents that are due but were never worked on
     * @param today          the current date
     * @return a positive number representing the health decrease
     */
    private int calculateHealthDecrease(final List<Content> newDueContents, final OffsetDateTime today) {
        final int baseHealthDecrease = newDueContents.stream()
                .mapToInt(content -> getDaysOverDue(content, today))
                .map(days -> days + 1) // on the day it is due, it should count as 1 day overdue
                .sum();

        return (int) Math.min(HEALTH_DECREASE_CAP,
                Math.floor(HEALTH_MODIFIER_PER_DAY * baseHealthDecrease));
    }

    private List<Content> getDueContentsThatWereNeverWorked(final List<Content> contents, final OffsetDateTime today) {
        return contents.stream()
                .filter(this::isContentNew)
                .filter(content -> isContentDue(content, today))
                .toList();
    }

    private boolean isContentNew(final Content content) {
        return !content.getUserProgressData().getIsLearned();
    }

    private boolean isContentDue(final Content content, final OffsetDateTime today) {
        final OffsetDateTime dueDate = content.getMetadata().getSuggestedDate();
        return dueDate != null && dueDate.isBefore(today);
    }

    /**
     * Returns the number of days a content is overdue.
     *
     * @param content the content
     * @param today   the current date
     * @return the number of days the content is overdue
     */
    private int getDaysOverDue(final Content content, final OffsetDateTime today) {
        final OffsetDateTime dueDate = content.getMetadata().getSuggestedDate();
        if (dueDate == null) {
            return 0;
        }
        return (int) Duration.between(today, dueDate).abs().toDays();
    }


}
