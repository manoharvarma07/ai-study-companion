package com.aistudy.backend.tutor;

import com.aistudy.backend.ai.AiClient;
import com.aistudy.backend.analytics.ActivityEventService;
import com.aistudy.backend.common.exception.ResourceNotFoundException;
import com.aistudy.backend.knowledge.RetrievalService;
import com.aistudy.backend.ai.QueryIntent;
import com.aistudy.backend.learning.LearningContextService;
import com.aistudy.backend.project.Project;
import com.aistudy.backend.project.ProjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TutorService {
    private static final Logger log = LoggerFactory.getLogger(TutorService.class);
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final ProjectService projects;
    private final RetrievalService retrieval;
    private final LearningContextService context;
    private final AiClient ai;
    private final ActivityEventService events;
    private final ObjectMapper mapper;

    public TutorService(ConversationRepository conversations, MessageRepository messages,
                        ProjectService projects, RetrievalService retrieval,
                        LearningContextService context, AiClient ai,
                        ActivityEventService events, ObjectMapper mapper) {
        this.conversations = conversations;
        this.messages = messages;
        this.projects = projects;
        this.retrieval = retrieval;
        this.context = context;
        this.ai = ai;
        this.events = events;
        this.mapper = mapper;
    }

    public record ChatRequest(UUID conversationId,
                              @NotBlank @Size(min = 1, max = 4000) String message) {}
    public record CitationDto(String material, Integer page) {}
    public record ChatResponse(String conversationId, String answer, boolean grounded,
                               List<CitationDto> citations) {}
    public record ConversationDto(String id, String title, String updatedAt) {}
    public record MessageDto(String id, String role, String content, Boolean grounded,
                             List<CitationDto> citations, String createdAt) {}

    @Transactional
    public ChatResponse chat(UUID userId, UUID projectId, ChatRequest req) {
        Project project = projects.requireOwned(userId, projectId);

        Conversation conv = resolveConversation(userId, project, req.conversationId(), req.message());

        Message userMsg = new Message();
        userMsg.setConversation(conv);
        userMsg.setProjectId(projectId);
        userMsg.setUserId(userId);
        userMsg.setRole("USER");
        userMsg.setContent(req.message());
        messages.save(userMsg);

        // Project-scoped RAG: only this project's chunks are searchable.
        // Intent-aware breadth: document-wide questions get page-level evidence.
        QueryIntent intent = QueryIntent.classify(req.message());
        log.info("[TUTOR] intent = {} for project {}", intent, projectId);
        List<RetrievalService.RetrievedChunk> hits =
                retrieval.retrieveForTutor(projectId, userId, req.message(), intent);
        log.info("[TUTOR] retrieved chunks = {} for project {}", hits.size(), projectId);

        List<AiClient.EvidenceChunk> evidence = hits.stream()
                .map(h -> new AiClient.EvidenceChunk(h.materialName(), h.pageNumber(), h.content()))
                .toList();

        // Last few turns only — never the full history.
        List<AiClient.ChatTurn> history = messages
                .findTop10ByConversationIdAndUserIdOrderByCreatedAtDesc(conv.getId(), userId)
                .stream().sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(m -> new AiClient.ChatTurn(m.getRole(), m.getContent()))
                .toList();

        AiClient.TutorAnswer answer = ai.generateTutorResponse(new AiClient.TutorPrompt(
                new AiClient.AiContext(userId, projectId, "TUTOR"),
                project.getName(), project.getGoal(),
                context.digest(userId, projectId), evidence, history, req.message()));

        Message aiMsg = new Message();
        aiMsg.setConversation(conv);
        aiMsg.setProjectId(projectId);
        aiMsg.setUserId(userId);
        aiMsg.setRole("ASSISTANT");
        aiMsg.setContent(answer.answer());
        aiMsg.setGrounded(answer.grounded());
        try {
            aiMsg.setCitations(mapper.writeValueAsString(answer.citations()));
        } catch (Exception e) {
            aiMsg.setCitations("[]");
        }
        messages.save(aiMsg);

        events.record(userId, projectId, "TUTOR_MESSAGE",
                Map.of("conversationId", conv.getId().toString(), "grounded", String.valueOf(answer.grounded())),
                "tutor-" + aiMsg.getId());

        return new ChatResponse(conv.getId().toString(), answer.answer(), answer.grounded(),
                answer.citations().stream()
                        .map(c -> new CitationDto(c.material(), c.page())).toList());
    }

    public List<ConversationDto> listConversations(UUID userId, UUID projectId) {
        projects.requireOwned(userId, projectId);
        return conversations.findByProjectIdAndUserIdOrderByUpdatedAtDesc(projectId, userId)
                .stream().map(c -> new ConversationDto(c.getId().toString(), c.getTitle(),
                        c.getUpdatedAt() == null ? null : c.getUpdatedAt().toString()))
                .toList();
    }

    public List<MessageDto> listMessages(UUID userId, UUID conversationId) {
        Conversation conv = conversations.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        return messages.findByConversationIdAndUserIdOrderByCreatedAtAsc(conv.getId(), userId)
                .stream().map(this::toDto).toList();
    }

    private Conversation resolveConversation(UUID userId, Project project, UUID conversationId, String firstMsg) {
        if (conversationId != null) {
            return conversations.findByIdAndProjectIdAndUserId(conversationId, project.getId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        }
        Conversation c = new Conversation();
        c.setUser(project.getUser());
        c.setProject(project);
        String title = firstMsg == null ? "New conversation"
                : (firstMsg.length() > 60 ? firstMsg.substring(0, 60) : firstMsg);
        c.setTitle(title);
        return conversations.save(c);
    }

    private MessageDto toDto(Message m) {
        List<CitationDto> cits = List.of();
        try {
            if (m.getCitations() != null && !m.getCitations().isBlank()) {
                cits = mapper.readValue(m.getCitations(),
                        mapper.getTypeFactory().constructCollectionType(List.class, CitationDto.class));
            }
        } catch (Exception ignored) {
        }
        return new MessageDto(m.getId().toString(), m.getRole(), m.getContent(),
                m.getGrounded(), cits,
                m.getCreatedAt() == null ? null : m.getCreatedAt().toString());
    }
}
