package de.unistuttgart.iste.gits.reward.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.Content;
import de.unistuttgart.iste.gits.generated.dto.RewardChangeReason;
import de.unistuttgart.iste.gits.reward.persistence.dao.AllRewardScoresEntity;
import de.unistuttgart.iste.gits.reward.persistence.dao.RewardScoreEntity;
import de.unistuttgart.iste.gits.reward.persistence.dao.RewardScoreLogEntry;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PowerScoreCalculator implements ScoreCalculator {

    public static final double HEALTH_FITNESS_MULTIPLIER = 0.1;
    @Override
    public RewardScoreEntity recalculateScore(AllRewardScoresEntity allRewardScores, List<Content> contents) {

        RewardScoreEntity growth = allRewardScores.getGrowth();
        RewardScoreEntity strength = allRewardScores.getStrength();
        RewardScoreEntity health = allRewardScores.getHealth();
        RewardScoreEntity fitness = allRewardScores.getFitness();
        RewardScoreEntity power = allRewardScores.getPower();


        double powerValueDouble = (growth.getValue() + strength.getValue()) + HEALTH_FITNESS_MULTIPLIER * (health.getValue() + fitness.getValue()) * (growth.getValue() + strength.getValue());
        int powerValueInt = (int) Math.round(powerValueDouble);

        RewardScoreLogEntry logEntry = RewardScoreLogEntry.builder()

                .reason(RewardChangeReason.COMPOSITE_VALUE)
                .build();

        power.setValue(powerValueInt);
        power.getLog().add(logEntry);

        return power;

    }

    @Override
    public RewardScoreEntity calculateOnContentWorkedOn(AllRewardScoresEntity allRewardScores, List<Content> contents, UserProgressLogEvent event) {


        RewardScoreEntity growth = allRewardScores.getGrowth();
        RewardScoreEntity strength = allRewardScores.getStrength();
        RewardScoreEntity health = allRewardScores.getHealth();
        RewardScoreEntity fitness = allRewardScores.getFitness();
        RewardScoreEntity power = allRewardScores.getPower();


        double powerValueDouble = (growth.getValue() + strength.getValue()) + HEALTH_FITNESS_MULTIPLIER * (health.getValue() + fitness.getValue()) * (growth.getValue() + strength.getValue());
        int powerValueInt = (int) Math.round(powerValueDouble);
        RewardScoreLogEntry logEntry = RewardScoreLogEntry.builder()

                .reason(RewardChangeReason.DONE_VALUE)
                .build();

        power.setValue(powerValueInt);
        power.getLog().add(logEntry);



        return power;
    }
}
