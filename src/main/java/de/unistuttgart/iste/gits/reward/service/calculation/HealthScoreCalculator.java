package de.unistuttgart.iste.gits.reward.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.RewardChangeReason;
import de.unistuttgart.iste.gits.reward.persistence.dao.RewardScoreEntity;
import de.unistuttgart.iste.gits.reward.persistence.dao.RewardScoreLogEntry;
import de.unistuttgart.iste.gits.reward.service.ContentWithUserProgressData;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.util.CollectionUtils.isEmpty;

public class HealthScoreCalculator implements ScoreCalculator {

    public static final double HEALTH_MODIFIER_PER_DAY = 0.5;
    public static final double HEALTH_DECREASE_CAP = -20.0;

    @Override
    public RewardScoreEntity recalculateScore(RewardScoreEntity rewardScore,
                                              List<ContentWithUserProgressData> contents) {
        int oldScore = rewardScore.getValue();
        OffsetDateTime today = OffsetDateTime.now();

        List<ContentWithUserProgressData> newDueContents = getDueContentsThatWereNeverWorked(contents, today);

        int diff = calculateHealthDecrease(newDueContents, today);

        if (diff == 0) {
            return rewardScore;
        }

        RewardScoreLogEntry logEntry = RewardScoreLogEntry.builder()
                .date(today)
                .difference(diff)
                .newValue(oldScore + diff)
                .oldValue(oldScore)
                .reason(RewardChangeReason.CONTENT_DUE_FOR_LEARNING)
                .associatedContents(getIds(newDueContents))
                .build();

        rewardScore.setValue(oldScore + diff);
        rewardScore.getLog().add(logEntry);

        return rewardScore;
    }

    @Override
    public RewardScoreEntity calculateOnContentWorkedOn(
            RewardScoreEntity rewardScore,
            List<ContentWithUserProgressData> contents,
            UserProgressLogEvent event) {

        int oldScore = rewardScore.getValue();
        int diffToFull = 100 - oldScore;

        if (diffToFull == 0) {
            return rewardScore;
        }

        OffsetDateTime today = OffsetDateTime.now();

        List<ContentWithUserProgressData> newDueContents = getDueContentsThatWereNeverWorked(contents, today);
        int numberOfNewDueContentsBefore = newDueContents.size();
        // this list might or might not include the content that was just worked on
        // depending on if the content service has already processed the event or not
        if (!doesListContainContentWithId(newDueContents, event.getContentId())) {
            numberOfNewDueContentsBefore++;
        }

        int healthIncrease = diffToFull / numberOfNewDueContentsBefore;
        int newValue = Math.min(oldScore + healthIncrease, 100);

        RewardScoreLogEntry logEntry = RewardScoreLogEntry.builder()
                .date(today)
                .difference(healthIncrease)
                .newValue(newValue)
                .oldValue(oldScore)
                .reason(RewardChangeReason.CONTENT_DONE)
                .associatedContents(List.of(event.getContentId()))
                .build();

        rewardScore.setValue(newValue);
        rewardScore.getLog().add(logEntry);

        return rewardScore;

    }

    private static boolean doesListContainContentWithId(List<ContentWithUserProgressData> newDueContents, UUID contentId) {
        return newDueContents.stream()
                .anyMatch(content -> content.getId().equals(contentId));
    }

    private static List<UUID> getIds(List<ContentWithUserProgressData> newDueContents) {
        return newDueContents.stream()
                .map(ContentWithUserProgressData::getId)
                .toList();
    }

    private int calculateHealthDecrease(List<ContentWithUserProgressData> newDueContents, OffsetDateTime today) {
        return (int) Math.max(HEALTH_DECREASE_CAP,
                Math.floor(HEALTH_MODIFIER_PER_DAY * newDueContents.stream()
                        .mapToInt(contentWithUserProgressData -> getDaysOverDue(contentWithUserProgressData, today))
                        .map(days -> days + 1) // on the day it is due, it should count as 1 day overdue
                        .sum()));
    }

    private List<ContentWithUserProgressData> getDueContentsThatWereNeverWorked(List<ContentWithUserProgressData> contents, OffsetDateTime today) {
        return contents.stream()
                .filter(this::isContentNotWorkedOn)
                .filter(contentWithUserProgressData -> isContentDue(contentWithUserProgressData, today))
                .toList();
    }

    private boolean isContentNotWorkedOn(ContentWithUserProgressData contentWithUserProgressData) {
        return isEmpty(contentWithUserProgressData.getUserProgressData().getLog());
    }

    private boolean isContentDue(ContentWithUserProgressData contentWithUserProgressData, OffsetDateTime today) {
        OffsetDateTime dueDate = contentWithUserProgressData.getMetadata().getSuggestedDate();
        return dueDate != null && dueDate.isBefore(today);
    }

    private int getDaysOverDue(ContentWithUserProgressData contentWithUserProgressData, OffsetDateTime today) {
        OffsetDateTime dueDate = contentWithUserProgressData.getMetadata().getSuggestedDate();
        if (dueDate == null) {
            return 0;
        }
        return (int) Duration.between(today, dueDate).toDays();
    }
}
