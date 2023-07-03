package de.unistuttgart.iste.gits.reward.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.Content;
import de.unistuttgart.iste.gits.reward.persistence.dao.RewardScoreEntity;

import java.util.List;

public class StrengthScoreCalculator implements ScoreCalculator {
    @Override
    public RewardScoreEntity recalculateScore(RewardScoreEntity rewardScore, List<Content> contents) {
        return rewardScore;
    }

    @Override
    public RewardScoreEntity calculateOnContentWorkedOn(RewardScoreEntity rewardScore, List<Content> contents, UserProgressLogEvent event) {
        return rewardScore;
    }
}
