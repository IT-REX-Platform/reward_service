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
    private static final double MAX_DECREASE_PER_DAY = 0.2; // 20%

    @Override
    public RewardScoreEntity recalculateScore(AllRewardScoresEntity allRewardScores, List<Content> contents) {
        RewardScoreEntity fitnessScoreBefore = allRewardScores.getFitness();
        OffsetDateTime today = OffsetDateTime.now();
        double fitnessDecrease = calculateFitnessDecrease(contents, today);
        double newFitnessScore = fitnessScoreBefore.getValue() * (1 - fitnessDecrease);

        int newFitnessScoreInt = (int) Math.round(newFitnessScore);
        int fitnessDecreaseValue = fitnessScoreBefore.getValue() - newFitnessScoreInt;
        int updatedFitnessScore = newFitnessScoreInt;

        RewardScoreLogEntry logEntry = RewardScoreLogEntry.builder()
                .date(OffsetDateTime.now())
                .difference(fitnessDecreaseValue)
                .oldValue(fitnessScoreBefore.getValue())
                .newValue(updatedFitnessScore)
                //.reason(RewardChangeReason.CONTENT_DONE) I AM NOT SURE WHAT COMES HERE?
                .associatedContentIds(Collections.emptyList())
                .build();

        fitnessScoreBefore.setValue(updatedFitnessScore);
        fitnessScoreBefore.getLog().add(logEntry);

        return fitnessScoreBefore;
    }

    @Override
    public RewardScoreEntity calculateOnContentWorkedOn(AllRewardScoresEntity allRewardScores, List<Content> contents, UserProgressLogEvent event) {
        RewardScoreEntity fitnessScoreBefore = allRewardScores.getFitness();
        OffsetDateTime today = OffsetDateTime.now();
        double fitnessDecrease = calculateFitnessDecrease(contents, today);
        double newFitnessScore = fitnessScoreBefore.getValue() * (1 - fitnessDecrease);

        int newFitnessScoreInt = (int) Math.round(newFitnessScore);
        int fitnessDecreaseValue = fitnessScoreBefore.getValue() - newFitnessScoreInt;
        int updatedFitnessScore = newFitnessScoreInt;

        RewardScoreLogEntry logEntry = RewardScoreLogEntry.builder()
                .date(OffsetDateTime.now())
                .difference(fitnessDecreaseValue)
                .oldValue(fitnessScoreBefore.getValue())
                .newValue(updatedFitnessScore)
                .reason(RewardChangeReason.CONTENT_REVIEWED)
                .associatedContentIds(Collections.emptyList())
                .build();

        fitnessScoreBefore.setValue(updatedFitnessScore);
        fitnessScoreBefore.getLog().add(logEntry);

        return fitnessScoreBefore;
    }

    private double calculateFitnessDecrease(List<Content> contents, OffsetDateTime today) {
        double fitnessDecrease = 0.0;

        for (Content content : contents) {
            if (isDueForRepetition(content)) {
                int daysOverdue = calculateDaysOverdue(content);
                double correctness = calculateCorrectnessModifier(content, today);
                double decreasePerDay = 1 + (2 * daysOverdue * (1 - Math.pow(correctness, 2)));
                fitnessDecrease += decreasePerDay;
            }
        }

        return Math.min(MAX_DECREASE_PER_DAY, fitnessDecrease);
    }

    private boolean isDueForRepetition(Content content) {
        OffsetDateTime today = OffsetDateTime.now();
        OffsetDateTime repetitionDate = content.getUserProgressData().getLastLearnDate();

        // Check if the repetition date is today or in the past
        return repetitionDate.isBefore(today) || repetitionDate.isEqual(today);
    }

    private int calculateDaysOverdue(Content content) {
        OffsetDateTime today = OffsetDateTime.now();
        OffsetDateTime repetitionDate = content.getUserProgressData().getLastLearnDate();

        // Calculate the number of days between the current date and the repetition date
        long daysBetween = ChronoUnit.DAYS.between(repetitionDate.toLocalDate(), today.toLocalDate());

        // If the content is due for repetition today, set daysOverdue to 1
        return daysBetween > 0 ? (int) daysBetween : 1;
    }

    private double calculateCorrectnessModifier(Content content, OffsetDateTime today) {
        OffsetDateTime learningDate = content.getUserProgressData().getLastLearnDate();

        long daysSinceLearning = ChronoUnit.DAYS.between(learningDate.toLocalDate(), today.toLocalDate());

        // Calculate the correctness modifier based on the days since learning
        double correctnessModifier = 1 - (0.1 * daysSinceLearning);

        // Ensure the correctness modifier is within the range of 0 to 1
        double correctness = Math.max(0, Math.min(1, correctnessModifier));

        // Square the correctness value to make the decrease more significant for low correctness
        return Math.pow(correctness, 2);
    }
    public double calculateFitnessRegeneration(double fitness, int contentsToRepeat, double correctnessBefore, double correctnessAfter) {
        double fitnessRegen = (1 + correctnessAfter - correctnessBefore) * (100 - fitness) / contentsToRepeat;
        return fitnessRegen;
    }
}
