package com.aistudy.backend.knowledge;

import com.aistudy.backend.ai.OpenAiClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Embedding facade. Delegates to the AI client (real API when configured,
 * deterministic offline vectors otherwise). The same function is used for
 * documents and queries so cosine search stays consistent.
 */
@Service
public class EmbeddingService {
    private final OpenAiClient ai;

    public EmbeddingService(OpenAiClient ai) {
        this.ai = ai;
    }

    public String embedToLiteral(String text) {
        List<Double> vec = ai.embed(text);
        return OpenAiClient.toVectorLiteral(vec);
    }
}
