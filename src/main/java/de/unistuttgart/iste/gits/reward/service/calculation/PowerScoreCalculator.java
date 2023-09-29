package de.unistuttgart.iste.gits.reward.service.calculation;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.generated.dto.Content;
import de.unistuttgart.iste.gits.generated.dto.RewardChangeReason;
import de.unistuttgart.iste.gits.reward.persistence.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Calculates the power score of a user, according the concept documented
 * <a href="https://gits-enpro.readthedocs.io/en/latest/dev-manuals/gamification/Scoring%20System.html#power">here</a>.
 */
@Component
@Slf4j
public class PowerScoreCalculator implements ScoreCalculator {

    private static final double HEALTH_FITNESS_MULTIPLIER_DEFAULT = 0.1;

    /**
     * The multiplier for the health and fitness score.
     * @see PowerScoreCalculator#PowerScoreCalculator(double)
     */
    private final double healthFitnessMultiplier;

    /**
     * Creates a new instance.
     *
     * @param healthFitnessMultiplier the health and fitness multiplier.
     *                                This controls how much the health and fitness score influence the power score.
     *                                By default, the health and fitness score can increase the power score by up to 10%.
     */
    @Autowired
    public PowerScoreCalculator(@Value("${reward.power.health_fitness_multiplier}") final double healthFitnessMultiplier) {
        log.info("Creating PowerScoreCalculator with healthFitnessMultiplier={}", healthFitnessMultiplier);
        this.healthFitnessMultiplier = healthFitnessMultiplier;
    }

    /**
     * Creates a new instance with default values.
     */
    public PowerScoreCalculator() {
        this(HEALTH_FITNESS_MULTIPLIER_DEFAULT);
    }

    @Override
    public RewardScoreEntity recalculateScore(final AllRewardScoresEntity allRewardScores,
                                              final List<Content> contents) {
        return calculatePowerScore(allRewardScores);
    }

    @Override
    public RewardScoreEntity calculateOnContentWorkedOn(final AllRewardScoresEntity allRewardScores,
                                                        final List<Content> contents,
                                                        final UserProgressLogEvent event) {
        return calculatePowerScore(allRewardScores);
    }

    private RewardScoreEntity calculatePowerScore(final AllRewardScoresEntity allRewardScores) {
        final int growth = allRewardScores.getGrowth().getValue();
        final int strength = allRewardScores.getStrength().getValue();
        final int health = allRewardScores.getHealth().getValue();
        final int fitness = allRewardScores.getFitness().getValue();
        final int oldPower = allRewardScores.getPower().getValue();

        final double powerValue = (growth + strength)
                                  + healthFitnessMultiplier * 0.01 * (health + fitness) * (growth + strength);
        final int newPower = (int) Math.round(powerValue);

        final int difference = newPower - oldPower;
        if (difference == 0) {
            // no change in power score, so no log entry is created
            return allRewardScores.getPower();
        }

        final RewardScoreLogEntry logEntry = createLogEntry(oldPower, newPower);

        final RewardScoreEntity powerEntity = allRewardScores.getPower();
        powerEntity.setValue(newPower);
        powerEntity.getLog().add(logEntry);

        return powerEntity;
    }

    private static RewardScoreLogEntry createLogEntry(final int oldPower, final int newPower) {
        return RewardScoreLogEntry.builder()
                .date(OffsetDateTime.now())
                .difference(newPower - oldPower)
                .oldValue(oldPower)
                .newValue(newPower)
                .reason(RewardChangeReason.COMPOSITE_VALUE)
                .associatedContentIds(Collections.emptyList())
                .build();
    }
}
