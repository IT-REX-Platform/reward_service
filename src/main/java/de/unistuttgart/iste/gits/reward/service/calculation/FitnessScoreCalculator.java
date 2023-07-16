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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class FitnessScoreCalculator implements ScoreCalculator {
    private static final double MAX_DECREASE_PER_DAY = 20;

    @Override
    public RewardScoreEntity recalculateScore(AllRewardScoresEntity allRewardScores, List<Content> contents) {
        RewardScoreEntity fitnessScoreBefore = allRewardScores.getFitness();

        OffsetDateTime today = OffsetDateTime.now();
        double fitnessDecrease = calculateFitnessDecrease(contents, today);
        double newFitnessScore = fitnessScoreBefore.getValue() - fitnessDecrease ;
        int intNewFitnessScore = (int) Math.round(newFitnessScore);

        RewardScoreLogEntry logEntry = RewardScoreLogEntry.builder()
                .date(OffsetDateTime.now())
                .difference(fitnessScoreBefore.getValue() - intNewFitnessScore)
                .oldValue(fitnessScoreBefore.getValue())
                .newValue(intNewFitnessScore)
                .reason(RewardChangeReason.CONTENT_REVIEWED)
                //.associatedContentIds(Collections.emptyList())
                .associatedContentIds(getIds(contents))
                .build();

        fitnessScoreBefore.setValue(intNewFitnessScore);
        fitnessScoreBefore.getLog().add(logEntry);
        fitnessScoreBefore.getLog().clear();

        System.out.println("Fitness Score Before: " + fitnessScoreBefore.getValue());
        System.out.println("Fitness Score Decrease: " + fitnessDecrease);
        System.out.println("New Fitness Score: " + newFitnessScore);

        return fitnessScoreBefore;
    }

    @Override
    public RewardScoreEntity calculateOnContentWorkedOn(AllRewardScoresEntity allRewardScores, List<Content> contents, UserProgressLogEvent event) {
        RewardScoreEntity fitnessScoreBefore = allRewardScores.getFitness();
        OffsetDateTime today = OffsetDateTime.now();
        double fitnessDecrease = calculateFitnessDecrease(contents, today);
        double newFitnessScore = fitnessScoreBefore.getValue() - fitnessDecrease;
        int intNewFitnessScore = (int) Math.round(newFitnessScore);

        double correctnessBefore = calculateCorrectnessBefore(contents);

        double correctnessAfter = event.getCorrectness();
        int contentsToRepeat = contents.size();

        double fitnessRegen = calculateFitnessRegeneration(newFitnessScore, contentsToRepeat, correctnessBefore, correctnessAfter);

        // Cap the fitness regeneration at 1% per repetition
        fitnessRegen = Math.min(fitnessRegen, 1);

        double updatedFitnessScore = newFitnessScore + fitnessRegen * contentsToRepeat;
        int intUpdatedFitnessScore = (int) Math.round(updatedFitnessScore);

        RewardScoreLogEntry logEntry = RewardScoreLogEntry.builder()
                .date(OffsetDateTime.now())
                .difference(intUpdatedFitnessScore - intNewFitnessScore)
                .oldValue(fitnessScoreBefore.getValue())
                .newValue(intUpdatedFitnessScore)
                .reason(RewardChangeReason.CONTENT_REVIEWED)
                .associatedContentIds(getIds(contents))
                .build();

        fitnessScoreBefore.setValue(intUpdatedFitnessScore);
        fitnessScoreBefore.getLog().add(logEntry);
        System.out.println("Fitness Score Before: " + fitnessScoreBefore.getValue());
        System.out.println("Fitness Decrease: " + fitnessDecrease);
        System.out.println("New Fitness Score: " + newFitnessScore);
        System.out.println("Correctness Before: " + correctnessBefore);
        System.out.println("Correctness After: " + correctnessAfter);
        System.out.println("Contents To Repeat: " + contentsToRepeat);
        System.out.println("Fitness Regen: " + fitnessRegen);
        System.out.println("Updated Fitness Score: " + updatedFitnessScore);

        return fitnessScoreBefore;
    }
    private List<UUID> getIds(List<Content> contents) {
        List<UUID> ids = new ArrayList<>();
        for (Content content : contents) {
            ids.add(content.getId());
        }
        return ids;
    }

    private double calculateFitnessDecrease(List<Content> contents, OffsetDateTime today) {
        double fitnessDecrease = 0.0;

        for (Content content : contents) {
            if (isDueForRepetition(content)) {
                int daysOverdue = calculateDaysOverdue(content);
                double correctness = calculateCorrectnessModifier(content, today);
                double decreasePerDay = 1 + (2 * daysOverdue * (1 - correctness));
                fitnessDecrease += decreasePerDay;

                System.out.println("Content: " + content.getId());
                System.out.println("Days Overdue: " + daysOverdue);
                System.out.println("Correctness: " + correctness);
                System.out.println("Decrease Per Day: " + decreasePerDay);
            }
        }

        System.out.println("Total Fitness Decrease: " + fitnessDecrease);

        return Math.min(MAX_DECREASE_PER_DAY, fitnessDecrease);
    }

    private boolean isDueForRepetition(Content content) {
        OffsetDateTime today = OffsetDateTime.now();
        OffsetDateTime repetitionDate = content.getUserProgressData().getLastLearnDate();

        System.out.println("Content: " + content.getId());
        System.out.println("Today: " + today);
        System.out.println("Repetition Date: " + repetitionDate);

        // Check if the repetition date is today or in the past
        boolean isDue = repetitionDate != null && (repetitionDate.isBefore(today) || repetitionDate.isEqual(today));

        System.out.println("Is Due for Repetition: " + isDue);

        return isDue;
    }


    private int calculateDaysOverdue(Content content) {
        OffsetDateTime today = OffsetDateTime.now();
        OffsetDateTime repetitionDate = content.getUserProgressData().getNextLearnDate();

        System.out.println("Content: " + content.getId());
        System.out.println("Today: " + today);
        System.out.println("Repetition Date: " + repetitionDate);

        // Calculate the number of days between the current date and the repetition date
        long daysBetween = ChronoUnit.DAYS.between(today, repetitionDate);

        System.out.println("Days Between: " + daysBetween);

        // If the content is due for repetition today, set daysOverdue to 1
        int daysOverdue = (int) Math.max(1, daysBetween);

        System.out.println("Days Overdue: " + daysOverdue);

        return daysOverdue;
    }


    private double calculateCorrectnessModifier(Content content, OffsetDateTime today) {
        OffsetDateTime learningDate = content.getUserProgressData().getLastLearnDate();

        System.out.println("Content: " + content.getId());
        System.out.println("Today: " + today);
        System.out.println("Learning Date: " + learningDate);

        if (learningDate == null) {
            return 0.0; // or handle the null case appropriately
        }

        long daysSinceLearning = ChronoUnit.DAYS.between(learningDate, today);

        System.out.println("Days Since Learning: " + daysSinceLearning);

        // Calculate the correctness modifier based on the days since learning
        double correctnessModifier = 1 - (0.1 * daysSinceLearning);

        System.out.println("Correctness Modifier: " + correctnessModifier);

        // Ensure the correctness modifier is within the range of 0 to 1
        double correctness = Math.max(0, Math.min(1, correctnessModifier));

        System.out.println("Correctness: " + correctness);

        // Square the correctness value to make the decrease more significant for low correctness
        return Math.pow(correctness, 2);
    }

    private double calculateCorrectnessBefore(List<Content> contents) {
        double correctnessSum = 0.0;

        for (Content content : contents) {
            double correctness = calculateCorrectnessModifier(content, OffsetDateTime.now());
            correctnessSum += correctness;
            System.out.println("Content: " + content.getId());
            System.out.println("Correctness: " + correctness);
        }

        double averageCorrectness = correctnessSum / contents.size();
        System.out.println("Average Correctness: " + averageCorrectness);

        return averageCorrectness;
    }


    private double calculateFitnessRegeneration(double fitness, int contentsToRepeat, double correctnessBefore, double correctnessAfter) {
        double fitnessRegen = (1 + correctnessAfter - correctnessBefore) * (100 - fitness) / contentsToRepeat;
        System.out.println("Fitness: " + fitness);
        System.out.println("Contents To Repeat: " + contentsToRepeat);
        System.out.println("Correctness Before: " + correctnessBefore);
        System.out.println("Correctness After: " + correctnessAfter);
        System.out.println("Fitness Regen: " + fitnessRegen);
        return fitnessRegen;
    }

}
