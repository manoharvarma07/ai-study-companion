package com.aistudy.backend.tutor;

import com.aistudy.backend.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class TutorController {
    private final TutorService service;

    public TutorController(TutorService service) {
        this.service = service;
    }

    @PostMapping("/api/projects/{projectId}/tutor/chat")
    public TutorService.ChatResponse chat(@PathVariable UUID projectId,
                                          @Valid @RequestBody TutorService.ChatRequest req) {
        return service.chat(CurrentUser.id(), projectId, req);
    }

    @GetMapping("/api/projects/{projectId}/tutor/conversations")
    public List<TutorService.ConversationDto> conversations(@PathVariable UUID projectId) {
        return service.listConversations(CurrentUser.id(), projectId);
    }

    @GetMapping("/api/conversations/{conversationId}/messages")
    public List<TutorService.MessageDto> messages(@PathVariable UUID conversationId) {
        return service.listMessages(CurrentUser.id(), conversationId);
    }
}
