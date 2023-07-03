package de.unistuttgart.iste.gits.reward.service;

import de.unistuttgart.iste.gits.generated.dto.ContentMetadata;
import de.unistuttgart.iste.gits.generated.dto.UserProgressData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContentWithUserProgressData {

    private UUID id;
    private ContentMetadata metadata;
    private UserProgressData userProgressData;
}
