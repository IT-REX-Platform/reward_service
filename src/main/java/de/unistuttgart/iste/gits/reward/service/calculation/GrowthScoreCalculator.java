package de.unistuttgart.iste.gits.reward.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.Content;
import de.unistuttgart.iste.gits.generated.dto.ProgressLogItem;
import de.unistuttgart.iste.gits.generated.dto.UserProgressData;
import de.unistuttgart.iste.gits.reward.persistence.dao.AllRewardScoresEntity;
import de.unistuttgart.iste.gits.reward.persistence.dao.RewardScoreEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Component
public class GrowthScoreCalculator implements ScoreCalculator {
    @Override
    public RewardScoreEntity recalculateScore(AllRewardScoresEntity allRewardScores, List<Content> contents) {
        RewardScoreEntity growthScoreBefore = allRewardScores.getGrowth();

        int currentScore = getCurrentScore(contents);
        int totalScore = getTotalScore(contents);
        growthScoreBefore.setValue(currentScore);
        growthScoreBefore.setPercentage(calculatePercentage(currentScore, totalScore));

        return growthScoreBefore;
    }

    @Override
    public RewardScoreEntity calculateOnContentWorkedOn(AllRewardScoresEntity allRewardScores, List<Content> contents, UserProgressLogEvent event) {
        RewardScoreEntity growthScoreBefore = allRewardScores.getGrowth();

        int currentScore = getCurrentScore(contents);
        int totalScore = getTotalScore(contents);
        growthScoreBefore.setValue(currentScore);
        growthScoreBefore.setPercentage(calculatePercentage(currentScore, totalScore));

        return growthScoreBefore;
    }

    private float calculatePercentage(int currentScore, int totalScore) {
        return (float) currentScore / totalScore;
    }

    private int getTotalScore(List<Content> contents) {
        return contents.stream()
                .mapToInt(content -> content.getMetadata().getRewardPoints())
                .sum();
    }

    private int getCurrentScore(List<Content> contents) {
        return contents.stream()
                .filter(content -> contentCompletedSuccessfully(content.getUserProgressData()))
                .mapToInt(content -> content.getMetadata().getRewardPoints())
                .sum();
    }

    private boolean contentCompletedSuccessfully(UserProgressData userProgressData) {
        ProgressLogItem newestLog = Collections.max(userProgressData.getLog(), Comparator.comparing(ProgressLogItem::getTimestamp));
        return newestLog.getSuccess();
    }
}
