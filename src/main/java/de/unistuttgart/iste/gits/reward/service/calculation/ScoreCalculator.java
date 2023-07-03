package de.unistuttgart.iste.gits.reward.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.reward.persistence.dao.RewardScoreEntity;
import de.unistuttgart.iste.gits.reward.service.ContentWithUserProgressData;

import java.util.List;

public interface ScoreCalculator {

    RewardScoreEntity recalculateScore(RewardScoreEntity rewardScore, List<ContentWithUserProgressData> contents);

    RewardScoreEntity calculateOnContentWorkedOn(RewardScoreEntity rewardScore,
                                                 List<ContentWithUserProgressData> contents,
                                                 UserProgressLogEvent event);
}
