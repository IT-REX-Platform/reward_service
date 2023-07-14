package de.unistuttgart.iste.gits.reward.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.Content;
import de.unistuttgart.iste.gits.generated.dto.RewardChangeReason;
import de.unistuttgart.iste.gits.reward.persistence.dao.AllRewardScoresEntity;
import de.unistuttgart.iste.gits.reward.persistence.dao.RewardScoreEntity;
import de.unistuttgart.iste.gits.reward.persistence.dao.RewardScoreLogEntry;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
@Component
public class FitnessScoreCalculator implements ScoreCalculator {
    private static final double MAX_DECREASE_PER_DAY = 20;

    @Override
    public RewardScoreEntity recalculateScore(AllRewardScoresEntity allRewardScores, List<Content> contents) {
        RewardScoreEntity fitnessScoreBefore = allRewardScores.getFitness();

        UserProgressLogEvent event = UserProgressLogEvent.builder().build();

        double fitnessDecrease = calculateFitnessDecrease(contents,event);
        double newFitnessScore = fitnessScoreBefore.getValue() * (1 - fitnessDecrease);
        int intnewFitnessScore= (int) Math.round(newFitnessScore);
        RewardScoreLogEntry logEntry = RewardScoreLogEntry.builder()
               .date(OffsetDateTime.now())
                .difference(fitnessScoreBefore.getValue() - intnewFitnessScore)
                .oldValue(fitnessScoreBefore.getValue())
                .newValue(intnewFitnessScore)
                .reason(RewardChangeReason.CONTENT_DUE_FOR_LEARNING)
                .associatedContentIds(Collections.emptyList())
                .build();

        fitnessScoreBefore.setValue(intnewFitnessScore);
        fitnessScoreBefore.getLog().add(logEntry);

        return fitnessScoreBefore;
    }

    @Override
    public RewardScoreEntity calculateOnContentWorkedOn(AllRewardScoresEntity allRewardScores, List<Content> contents, UserProgressLogEvent event) {
        RewardScoreEntity fitnessScoreBefore = allRewardScores.getFitness();

        double fitnessDecrease = calculateFitnessDecrease(contents, event);
        double newFitnessScore = fitnessScoreBefore.getValue() * (1 - fitnessDecrease);
        int newFitnessScoreInt = (int) Math.round(newFitnessScore);

        // Decrease the fitness score
        int fitnessDecreaseValue = fitnessScoreBefore.getValue() - newFitnessScoreInt;
        int updatedFitnessScore = fitnessScoreBefore.getValue() - fitnessDecreaseValue;

        RewardScoreLogEntry logEntry = RewardScoreLogEntry.builder()
                .date(OffsetDateTime.now())
                .difference(fitnessDecreaseValue)
                .oldValue(fitnessScoreBefore.getValue())
                .newValue(updatedFitnessScore)
                .reason(RewardChangeReason.CONTENT_DONE)
                .associatedContentIds(Collections.emptyList())
                .build();

        fitnessScoreBefore.setValue(updatedFitnessScore);
        fitnessScoreBefore.getLog().add(logEntry);

        return fitnessScoreBefore;
    }


    private double calculateFitnessDecrease(List<Content> contents, UserProgressLogEvent event) {
        double fitnessDecrease = 0.0;

        for (Content content : contents) {
            if (isDueForRepetition(content)) {
                int daysOverdue = calculateDaysOverdue(content);
                double correctness = calculateCorrectnessModifier(event);
                double decreasePerDay = 1 + (2 * daysOverdue * (1 - correctness));
                fitnessDecrease += decreasePerDay;
            }
        }
        return Math.min(MAX_DECREASE_PER_DAY, fitnessDecrease);
    }
    private boolean isDueForRepetition(Content content) {
        // Implement the logic to determine if the content is due for repetition
        // Return true if it is due, false otherwise
        OffsetDateTime today = OffsetDateTime.now();
        OffsetDateTime repetitionDate = content.getUserProgressData().getLastLearnDate();

        // Check if the repetition date is today or in the past
        boolean isDueForRepetition = repetitionDate.isBefore(today) || repetitionDate.isEqual(today);

        return isDueForRepetition;
    }
    private int calculateDaysOverdue(Content content) {

        OffsetDateTime today = OffsetDateTime.now();
        OffsetDateTime repetitionDate = content.getMetadata().getSuggestedDate();

        // Calculate the number of days between the current date and the repetition date
        long daysBetween = ChronoUnit.DAYS.between(repetitionDate, today);

        // If the content is due for repetition today, set daysOverdue to 1
        int daysOverdue = daysBetween > 0 ? (int) daysBetween : 1;

        return daysOverdue;
    }
    private double calculateCorrectnessModifier(UserProgressLogEvent event) {
        // Implement the logic to calculate the correctness of the content
        // Return the correctness value between 0 and 1

        double correctness = event.getCorrectness();

        // Square the correctness value to make the decrease more significant for low correctness
        double squaredCorrectness = Math.pow(correctness, 2);

        return squaredCorrectness;
    }
}
