package com.aistudy.backend.admin;

import com.aistudy.backend.analytics.ActivityEvent;
import com.aistudy.backend.analytics.ActivityEventRepository;
import com.aistudy.backend.analytics.AiUsage;
import com.aistudy.backend.analytics.AiUsageRepository;
import com.aistudy.backend.material.Material;
import com.aistudy.backend.material.MaterialRepository;
import com.aistudy.backend.project.Project;
import com.aistudy.backend.project.ProjectRepository;
import com.aistudy.backend.quiz.QuizRepository;
import com.aistudy.backend.space.Space;
import com.aistudy.backend.space.SpaceRepository;
import com.aistudy.backend.tutor.ConversationRepository;
import com.aistudy.backend.tutor.MessageRepository;
import com.aistudy.backend.user.User;
import com.aistudy.backend.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserRepository users;
    private final SpaceRepository spaces;
    private final ProjectRepository projects;
    private final MaterialRepository materials;
    private final ActivityEventRepository events;
    private final AiUsageRepository aiUsage;
    private final QuizRepository quizzes;
    private final ConversationRepository conversations;
    private final MessageRepository messages;

    public AdminController(UserRepository users, SpaceRepository spaces, ProjectRepository projects,
                           MaterialRepository materials, ActivityEventRepository events,
                           AiUsageRepository aiUsage, QuizRepository quizzes,
                           ConversationRepository conversations, MessageRepository messages) {
        this.users = users;
        this.spaces = spaces;
        this.projects = projects;
        this.materials = materials;
        this.events = events;
        this.aiUsage = aiUsage;
        this.quizzes = quizzes;
        this.conversations = conversations;
        this.messages = messages;
    }

    static String providerFor(String model) {
        if (model == null || model.isBlank()) return "unknown";
        String m = model.toLowerCase();
        if (m.startsWith("offline-") || m.startsWith("heuristic")) return "offline";
        return "openrouter";
    }

    // ---------- overview ----------
    @GetMapping("/overview")
    public Map<String, Object> overview() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalUsers", users.count());
        out.put("activeUsers", countActiveUsers());
        out.put("totalSpaces", spaces.count());
        out.put("totalProjects", projects.count());
        out.put("totalMaterials", materials.count());
        return out;
    }

    private long countActiveUsers() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        java.util.Set<UUID> active = new java.util.HashSet<>();
        for (ActivityEvent e : events.findAll()) {
            if (e.getCreatedAt() != null && e.getCreatedAt().isAfter(cutoff) && e.getUserId() != null) {
                active.add(e.getUserId());
            }
        }
        for (AiUsage u : aiUsage.findAll()) {
            if (u.getCreatedAt() != null && u.getCreatedAt().isAfter(cutoff) && u.getUserId() != null) {
                active.add(u.getUserId());
            }
        }
        return active.size();
    }

    // ---------- users ----------
    @GetMapping("/users")
    public Map<String, Object> users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        List<User> all = users.findAll();
        String q = search == null ? "" : search.trim().toLowerCase();
        List<User> filtered = all.stream()
                .filter(u -> q.isEmpty()
                        || (u.getName() != null && u.getName().toLowerCase().contains(q))
                        || (u.getEmail() != null && u.getEmail().toLowerCase().contains(q)))
                .filter(u -> role == null || role.isBlank() || role.equalsIgnoreCase("ALL")
                        || u.getRole().name().equalsIgnoreCase(role))
                .collect(Collectors.toList());

        List<Map<String, Object>> rows = filtered.stream()
                .map(this::userRow)
                .collect(Collectors.toList());

        Comparator<Map<String, Object>> cmp = switch (sort) {
            case "name" -> Comparator.comparing(m -> String.valueOf(m.get("name")).toLowerCase());
            case "email" -> Comparator.comparing(m -> String.valueOf(m.get("email")).toLowerCase());
            case "projects" -> Comparator.comparing(m -> (Long) m.get("projects"));
            case "lastActivity" -> Comparator.comparing(m -> String.valueOf(m.get("lastActivity")));
            default -> Comparator.comparing(m -> String.valueOf(m.get("createdAt")));
        };
        if ("desc".equalsIgnoreCase(direction)) cmp = cmp.reversed();
        rows.sort(cmp);

        int s = Math.min(Math.max(1, size), 100);
        int p = Math.max(0, page);
        int from = Math.min(p * s, rows.size());
        int to = Math.min(from + s, rows.size());
        Page<Map<String, Object>> result = new PageImpl<>(rows.subList(from, to),
                PageRequest.of(p, s), rows.size());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("content", result.getContent());
        out.put("totalElements", result.getTotalElements());
        out.put("totalPages", result.getTotalPages());
        out.put("number", result.getNumber());
        out.put("size", result.getSize());
        return out;
    }

    private Map<String, Object> userRow(User u) {
        UUID id = u.getId();
        long spaceCount = safe(() -> spaces.countByUserId(id));
        long projectCount = safe(() -> projects.countByUserId(id));
        long materialCount = safe(() -> materials.countByUserId(id));
        long quizCount = safe(() -> quizzes.countByUserId(id));
        long tutorMessages = safe(() -> messages.countByUserId(id));
        Instant last = lastActivityFor(id);
        boolean active = last != null && last.isAfter(Instant.now().minus(30, ChronoUnit.DAYS));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id.toString());
        m.put("name", u.getName());
        m.put("email", u.getEmail());
        m.put("role", u.getRole().name());
        m.put("createdAt", u.getCreatedAt() == null ? null : u.getCreatedAt().toString());
        m.put("spaces", spaceCount);
        m.put("projects", projectCount);
        m.put("materials", materialCount);
        m.put("quizzes", quizCount);
        m.put("tutorMessages", tutorMessages);
        m.put("lastActivity", last == null ? null : last.toString());
        m.put("status", active ? "Active" : "Inactive");
        return m;
    }

    private Instant lastActivityFor(UUID userId) {
        Instant last = null;
        try {
            var e = events.findTop1ByUserIdOrderByCreatedAtDesc(userId);
            if (e.isPresent() && e.get().getCreatedAt() != null) last = e.get().getCreatedAt();
        } catch (Exception ignored) {}
        try {
            var a = aiUsage.findTop1ByUserIdOrderByCreatedAtDesc(userId);
            if (a.isPresent() && a.get().getCreatedAt() != null) {
                if (last == null || a.get().getCreatedAt().isAfter(last)) last = a.get().getCreatedAt();
            }
        } catch (Exception ignored) {}
        return last;
    }

    private long safe(java.util.function.LongSupplier s) {
        try { return s.getAsLong(); } catch (Exception e) { return 0; }
    }

    @GetMapping("/users/{id}")
    public Map<String, Object> userDetail(@PathVariable UUID id) {
        User u = users.findById(id).orElseThrow(() ->
                new com.aistudy.backend.common.exception.ResourceNotFoundException("User not found"));
        Map<String, Object> out = userRow(u);
        List<Map<String, Object>> spaceList = new ArrayList<>();
        try {
            for (Space s : spaces.findByUserIdOrderByCreatedAtDesc(id)) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", s.getId().toString());
                m.put("name", s.getName());
                m.put("createdAt", s.getCreatedAt() == null ? null : s.getCreatedAt().toString());
                spaceList.add(m);
            }
        } catch (Exception ignored) {}
        out.put("spaceList", spaceList);
        List<Map<String, Object>> projectList = new ArrayList<>();
        try {
            for (Project pr : projects.findAll().stream()
                    .filter(x -> x.getUser() != null && id.equals(x.getUser().getId())).toList()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", pr.getId().toString());
                m.put("name", pr.getName());
                m.put("spaceId", pr.getSpace() == null ? null : pr.getSpace().getId().toString());
                m.put("createdAt", pr.getCreatedAt() == null ? null : pr.getCreatedAt().toString());
                projectList.add(m);
            }
        } catch (Exception ignored) {}
        out.put("projectList", projectList);
        List<Map<String, Object>> recent = new ArrayList<>();
        try {
            for (ActivityEvent e : events.findTop20ByUserIdOrderByCreatedAtDesc(id)) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("type", e.getEventType());
                m.put("projectId", e.getProjectId() == null ? null : e.getProjectId().toString());
                m.put("at", e.getCreatedAt() == null ? null : e.getCreatedAt().toString());
                recent.add(m);
            }
        } catch (Exception ignored) {}
        out.put("recentActivity", recent);
        List<Map<String, Object>> recentAi = new ArrayList<>();
        try {
            for (AiUsage a : aiUsage.findTop20ByUserIdOrderByCreatedAtDesc(id)) {
                recentAi.add(toEventMap(a, u.getName(), u.getEmail()));
            }
        } catch (Exception ignored) {}
        out.put("recentAiUsage", recentAi);
        out.put("quizCount", safe(() -> quizzes.countByUserId(id)));
        out.put("conversationCount", safe(() -> conversations.countByUserId(id)));
        out.put("tutorMessageCount", safe(() -> messages.countByUserId(id)));
        return out;
    }

    @GetMapping("/spaces")
    public Object allSpaces(@RequestParam(defaultValue = "0") int page) {
        return spaces.findAll(PageRequest.of(Math.max(0, page), 50)).map(s -> Map.of(
                "id", s.getId().toString(), "name", s.getName(),
                "userId", s.getUser().getId().toString()));
    }

    @GetMapping("/projects")
    public Object allProjects(@RequestParam(defaultValue = "0") int page) {
        return projects.findAll(PageRequest.of(Math.max(0, page), 50)).map(p -> Map.of(
                "id", p.getId().toString(), "name", p.getName(),
                "userId", p.getUser().getId().toString()));
    }

    @GetMapping("/activity")
    public Object activity() {
        return events.findAll(PageRequest.of(0, 100)).map(e -> Map.of(
                "type", e.getEventType(),
                "userId", e.getUserId().toString(),
                "projectId", e.getProjectId() == null ? "" : e.getProjectId().toString(),
                "at", e.getCreatedAt().toString()));
    }

    @GetMapping("/analytics")
    public Map<String, Object> analytics() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalUsers", users.count());
        out.put("totalSpaces", spaces.count());
        out.put("totalProjects", projects.count());
        out.put("totalMaterials", materials.count());
        out.put("materialsReady", materials.findAll().stream()
                .filter(m -> m.getStatus() == Material.MaterialStatus.READY).count());
        out.put("materialsFailed", materials.findAll().stream()
                .filter(m -> m.getStatus() == Material.MaterialStatus.FAILED).count());
        out.put("aiUsageByFeature", aiUsage.usageByFeature().stream().map(f -> Map.of(
                "feature", f.getFeature(), "calls", f.getCalls(),
                "tokens", f.getTokens(), "cost", Math.round(f.getCost() * 10000.0) / 10000.0)).toList());
        return out;
    }

    // ---------- AI usage ----------
    private List<AiUsage> filteredAiUsage(String days, String feature, String model,
                                          String provider, String userId, String status) {
        Instant cutoff = cutoffFor(days);
        UUID uid = null;
        if (userId != null && !userId.isBlank() && !"ALL".equalsIgnoreCase(userId)) {
            try { uid = UUID.fromString(userId); } catch (Exception ignored) { return List.of(); }
        }
        UUID filterUid = uid;
        return aiUsage.findAll().stream()
                .filter(u -> cutoff == null || (u.getCreatedAt() != null && !u.getCreatedAt().isBefore(cutoff)))
                .filter(u -> feature == null || feature.isBlank() || "ALL".equalsIgnoreCase(feature)
                        || feature.equalsIgnoreCase(u.getFeature()))
                .filter(u -> model == null || model.isBlank() || "ALL".equalsIgnoreCase(model)
                        || model.equalsIgnoreCase(u.getModel()))
                .filter(u -> provider == null || provider.isBlank() || "ALL".equalsIgnoreCase(provider)
                        || provider.equalsIgnoreCase(providerFor(u.getModel())))
                .filter(u -> filterUid == null || filterUid.equals(u.getUserId()))
                .filter(u -> status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)
                        || ("SUCCESS".equalsIgnoreCase(status) && u.isSuccess())
                        || ("FAILED".equalsIgnoreCase(status) && !u.isSuccess()))
                .collect(Collectors.toList());
    }

    private Instant cutoffFor(String days) {
        if (days == null || days.isBlank() || "ALL".equalsIgnoreCase(days) || "all".equals(days)) return null;
        try {
            int d = Integer.parseInt(days);
            return Instant.now().minus(d, ChronoUnit.DAYS);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Map<String, String> userLookup() {
        Map<String, String> names = new LinkedHashMap<>();
        for (User u : users.findAll()) {
            names.put(u.getId().toString() + "|name", u.getName());
            names.put(u.getId().toString() + "|email", u.getEmail());
        }
        return names;
    }

    private Map<String, Object> toEventMap(AiUsage u, String userName, String userEmail) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId().toString());
        m.put("time", u.getCreatedAt() == null ? null : u.getCreatedAt().toString());
        m.put("at", u.getCreatedAt() == null ? null : u.getCreatedAt().toString());
        m.put("userId", u.getUserId() == null ? null : u.getUserId().toString());
        m.put("userName", userName);
        m.put("userEmail", userEmail);
        m.put("projectId", u.getProjectId() == null ? null : u.getProjectId().toString());
        m.put("feature", u.getFeature());
        m.put("provider", providerFor(u.getModel()));
        m.put("model", u.getModel());
        m.put("latencyMs", u.getLatencyMs());
        m.put("inputTokens", u.getInputTokens());
        m.put("outputTokens", u.getOutputTokens());
        long tokens = (long) u.getInputTokens() + u.getOutputTokens();
        m.put("tokens", tokens);
        m.put("totalTokens", tokens);
        m.put("cost", u.getEstimatedCost());
        m.put("estimatedCost", u.getEstimatedCost());
        m.put("success", u.isSuccess());
        m.put("status", u.isSuccess() ? "SUCCESS" : "FAILED");
        return m;
    }

    @GetMapping("/ai-usage")
    public Object aiUsage(
            @RequestParam(required = false) String days,
            @RequestParam(required = false) String feature,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status) {
        boolean hasFilter = days != null || feature != null || model != null
                || provider != null || userId != null || status != null;
        if (!hasFilter) {
            // Legacy shape, but with full fields so old clients never see [object Object] ambiguity.
            Map<String, String> lookup = userLookup();
            return aiUsage.findTop100ByOrderByCreatedAtDesc().stream()
                    .map(u -> {
                        String uid = u.getUserId() == null ? null : u.getUserId().toString();
                        return toEventMap(u,
                                uid == null ? null : lookup.get(uid + "|name"),
                                uid == null ? null : lookup.get(uid + "|email"));
                    }).toList();
        }
        return recent(days, feature, model, provider, userId, status, 0, 100).get("content");
    }

    @GetMapping("/ai-usage/summary")
    public Map<String, Object> aiSummary(
            @RequestParam(required = false) String days,
            @RequestParam(required = false) String feature,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status) {
        List<AiUsage> list = filteredAiUsage(days, feature, model, provider, userId, status);
        long total = list.size();
        long ok = list.stream().filter(AiUsage::isSuccess).count();
        long failed = total - ok;
        long tokens = list.stream().mapToLong(u -> (long) u.getInputTokens() + u.getOutputTokens()).sum();
        long in = list.stream().mapToLong(AiUsage::getInputTokens).sum();
        long out = list.stream().mapToLong(AiUsage::getOutputTokens).sum();
        double cost = list.stream().mapToDouble(AiUsage::getEstimatedCost).sum();
        double avgLatency = list.isEmpty() ? 0 : list.stream().mapToLong(AiUsage::getLatencyMs).average().orElse(0);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalRequests", total);
        m.put("successfulRequests", ok);
        m.put("failedRequests", failed);
        m.put("successRate", total == 0 ? 0 : Math.round(ok * 1000.0 / total) / 10.0);
        m.put("totalTokens", tokens);
        m.put("inputTokens", in);
        m.put("outputTokens", out);
        m.put("estimatedCost", Math.round(cost * 10000.0) / 10000.0);
        m.put("avgLatencyMs", Math.round(avgLatency * 10.0) / 10.0);
        return m;
    }

    @GetMapping("/ai-usage/timeline")
    public List<Map<String, Object>> timeline(
            @RequestParam(required = false, defaultValue = "30") String days,
            @RequestParam(required = false) String feature,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status) {
        int d = 30;
        try { d = Integer.parseInt(days); } catch (Exception ignored) {}
        d = Math.min(Math.max(1, d), 90);
        List<AiUsage> list = filteredAiUsage(String.valueOf(d), feature, model, provider, userId, status);
        Map<LocalDate, List<AiUsage>> byDay = list.stream()
                .filter(u -> u.getCreatedAt() != null)
                .collect(Collectors.groupingBy(u -> u.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate()));
        List<Map<String, Object>> out = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (int i = d - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            List<AiUsage> bucket = byDay.getOrDefault(day, List.of());
            long tokens = bucket.stream().mapToLong(u -> (long) u.getInputTokens() + u.getOutputTokens()).sum();
            double cost = bucket.stream().mapToDouble(AiUsage::getEstimatedCost).sum();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", day.toString());
            m.put("requests", bucket.size());
            m.put("tokens", tokens);
            m.put("cost", Math.round(cost * 10000.0) / 10000.0);
            out.add(m);
        }
        return out;
    }

    @GetMapping("/ai-usage/by-feature")
    public List<Map<String, Object>> byFeature(
            @RequestParam(required = false) String days,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status) {
        List<AiUsage> list = filteredAiUsage(days, null, model, provider, userId, status);
        return list.stream().collect(Collectors.groupingBy(AiUsage::getFeature)).entrySet().stream()
                .map(e -> {
                    List<AiUsage> g = e.getValue();
                    long tokens = g.stream().mapToLong(u -> (long) u.getInputTokens() + u.getOutputTokens()).sum();
                    double cost = g.stream().mapToDouble(AiUsage::getEstimatedCost).sum();
                    double avg = g.stream().mapToLong(AiUsage::getLatencyMs).average().orElse(0);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("feature", e.getKey());
                    m.put("requests", g.size());
                    m.put("tokens", tokens);
                    m.put("cost", Math.round(cost * 10000.0) / 10000.0);
                    m.put("avgLatencyMs", Math.round(avg * 10.0) / 10.0);
                    return m;
                })
                .sorted((a, b) -> Long.compare(((Number) b.get("requests")).longValue(), ((Number) a.get("requests")).longValue()))
                .toList();
    }

    @GetMapping("/ai-usage/by-model")
    public List<Map<String, Object>> byModel(
            @RequestParam(required = false) String days,
            @RequestParam(required = false) String feature,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status) {
        List<AiUsage> list = filteredAiUsage(days, feature, null, provider, userId, status);
        return list.stream().collect(Collectors.groupingBy(AiUsage::getModel)).entrySet().stream()
                .map(e -> {
                    List<AiUsage> g = e.getValue();
                    long tokens = g.stream().mapToLong(u -> (long) u.getInputTokens() + u.getOutputTokens()).sum();
                    double cost = g.stream().mapToDouble(AiUsage::getEstimatedCost).sum();
                    double avg = g.stream().mapToLong(AiUsage::getLatencyMs).average().orElse(0);
                    long ok = g.stream().filter(AiUsage::isSuccess).count();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("provider", providerFor(e.getKey()));
                    m.put("model", e.getKey());
                    m.put("requests", g.size());
                    m.put("tokens", tokens);
                    m.put("successRate", g.isEmpty() ? 0 : Math.round(ok * 1000.0 / g.size()) / 10.0);
m.put("avgLatencyMs", Math.round(avg * 10.0) / 10.0);
            m.put("cost", Math.round(cost * 10000.0) / 10000.0);
            m.put("estimatedCost", Math.round(cost * 10000.0) / 10000.0);
            return m;
        })
        .sorted((a, b) -> Long.compare(((Number) b.get("requests")).longValue(), ((Number) a.get("requests")).longValue()))
        .toList();
}

    @GetMapping("/ai-usage/by-user")
    public List<Map<String, Object>> byUser(
            @RequestParam(required = false) String days,
            @RequestParam(required = false) String feature,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String status) {
        List<AiUsage> list = filteredAiUsage(days, feature, model, provider, null, status);
        Map<UUID, List<AiUsage>> grouped = list.stream()
                .filter(u -> u.getUserId() != null)
                .collect(Collectors.groupingBy(AiUsage::getUserId));
        Map<UUID, User> userMap = users.findAll().stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        return grouped.entrySet().stream().map(e -> {
            List<AiUsage> g = e.getValue();
            long tokens = g.stream().mapToLong(u -> (long) u.getInputTokens() + u.getOutputTokens()).sum();
            double cost = g.stream().mapToDouble(AiUsage::getEstimatedCost).sum();
            Instant last = g.stream().map(AiUsage::getCreatedAt)
                    .filter(x -> x != null).max(Comparator.naturalOrder()).orElse(null);
            User u = userMap.get(e.getKey());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", e.getKey().toString());
            m.put("name", u == null ? "Unknown" : u.getName());
            m.put("email", u == null ? null : u.getEmail());
            m.put("requests", g.size());
            m.put("tokens", tokens);
            m.put("cost", Math.round(cost * 10000.0) / 10000.0);
            m.put("estimatedCost", Math.round(cost * 10000.0) / 10000.0);
            m.put("lastUsed", last == null ? null : last.toString());
            return m;
        }).sorted((a, b) -> Long.compare(((Number) b.get("requests")).longValue(), ((Number) a.get("requests")).longValue())).toList();
    }

    @GetMapping("/ai-usage/recent")
    public Map<String, Object> recent(
            @RequestParam(required = false) String days,
            @RequestParam(required = false) String feature,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        List<AiUsage> list = filteredAiUsage(days, feature, model, provider, userId, status);
        list.sort(Comparator.comparing(AiUsage::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        Map<String, String> lookup = userLookup();
        List<Map<String, Object>> rows = list.stream().map(u -> {
            String uid = u.getUserId() == null ? null : u.getUserId().toString();
            return toEventMap(u,
                    uid == null ? null : lookup.get(uid + "|name"),
                    uid == null ? null : lookup.get(uid + "|email"));
        }).toList();
        int s = Math.min(Math.max(1, size), 100);
        int p = Math.max(0, page);
        int from = Math.min(p * s, rows.size());
        int to = Math.min(from + s, rows.size());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("content", rows.subList(from, to));
        out.put("totalElements", rows.size());
        out.put("totalPages", (int) Math.ceil(rows.size() / (double) s));
        out.put("number", p);
        out.put("size", s);
        return out;
    }

    @GetMapping("/ai-usage/filter-options")
    public Map<String, Object> filterOptions() {
        List<AiUsage> all = aiUsage.findAll();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("features", all.stream().map(AiUsage::getFeature)
                .filter(x -> x != null).distinct().sorted().toList());
        out.put("models", all.stream().map(AiUsage::getModel)
                .filter(x -> x != null).distinct().sorted().toList());
        out.put("providers", all.stream().map(u -> providerFor(u.getModel()))
                .filter(x -> x != null).distinct().sorted().toList());
        return out;
    }

    @GetMapping("/material-processing")
    public Object materialProcessing() {
        return materials.findAll().stream()
                .filter(m -> m.getStatus() != Material.MaterialStatus.READY)
                .map(m -> Map.of(
                        "id", m.getId().toString(), "filename", m.getFilename(),
                        "status", m.getStatus().name(), "attempts", m.getProcessingAttempts(),
                        "error", m.getErrorMessage() == null ? "" : m.getErrorMessage()))
                .toList();
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", "UP");
        out.put("db", "UP");
        out.put("users", users.count());
        return out;
    }
}
