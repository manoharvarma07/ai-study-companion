package com.aistudy.backend.knowledge;

import com.aistudy.backend.ai.AiClient;
import com.aistudy.backend.project.Project;
import com.aistudy.backend.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Extracts key concepts per project after document processing.
 * De-duplicates case-insensitively within a project.
 */
@Service
public class ConceptService {
    private static final Logger log = LoggerFactory.getLogger(ConceptService.class);
    private final ConceptRepository concepts;
    private final AiClient ai;

    public ConceptService(ConceptRepository concepts, AiClient ai) {
        this.concepts = concepts;
        this.ai = ai;
    }

    public List<Concept> list(UUID projectId, UUID userId) {
        return concepts.findByProjectIdAndUserIdOrderByName(projectId, userId);
    }

    @Transactional
    public List<Concept> extractAndStore(UUID userId, UUID projectId,
                                        String materialName, String sampleText) {
        List<AiClient.ExtractedConcept> found;
        try {
            found = ai.extractConcepts(new AiClient.ConceptPrompt(
                    new AiClient.AiContext(userId, projectId, "CONCEPT_EXTRACTION"),
                    materialName, sampleText));
        } catch (Exception e) {
            log.warn("Concept extraction failed for {}: {}", materialName, e.getMessage());
            return List.of();
        }
        Project projectStub = new Project();
        projectStub.setId(projectId);
        User userStub = new User();
        userStub.setId(userId);
        return found.stream()
                .filter(c -> c.name() != null && !c.name().isBlank())
                .map(c -> {
                    String name = c.name().strip();
                    if (name.length() > 300) {
                        name = name.substring(0, 300);
                    }
                    final String finalName = name;
                    return concepts.findByProjectIdAndUserIdAndNameIgnoreCase(projectId, userId, finalName)
                            .orElseGet(() -> {
                                Concept concept = new Concept();
                                concept.setProject(projectStub);
                                concept.setUser(userStub);
                                concept.setName(finalName);
                                concept.setDescription(c.description());
                                return concepts.save(concept);
                            });
                }).toList();
    }
}
