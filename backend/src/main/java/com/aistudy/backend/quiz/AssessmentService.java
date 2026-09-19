package com.aistudy.backend.quiz;

import com.aistudy.backend.ai.AiClient;
import com.aistudy.backend.ai.AssessmentResult;
import com.aistudy.backend.analytics.ActivityEventService;
import com.aistudy.backend.common.exception.ResourceNotFoundException;
import com.aistudy.backend.knowledge.Concept;
import com.aistudy.backend.knowledge.ConceptRepository;
import com.aistudy.backend.project.Project;
import com.aistudy.backend.user.User;
import com.aistudy.backend.knowledge.RetrievalService;
import com.aistudy.backend.learning.LearningContextService;
import com.aistudy.backend.learning.MasteryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Evaluates answers (MCQ exact-match + AI/open-ended assessment),
 * updates estimated mastery, and refreshes learner context.
 */
@Service
public class AssessmentService {
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final ConceptRepository concepts;
    private final AiClient ai;
    private final MasteryService mastery;
    private final LearningContextService context;
    private final ActivityEventService events;
    private final ObjectMapper mapper;

    public AssessmentService(QuestionRepository questions, AnswerRepository answers,
                             ConceptRepository concepts, AiClient ai, MasteryService mastery,
                             LearningContextService context, ActivityEventService events,
                             ObjectMapper mapper) {
        this.questions = questions;
        this.answers = answers;
        this.concepts = concepts;
        this.ai = ai;
        this.mastery = mastery;
        this.context = context;
        this.events = events;
        this.mapper = mapper;
    }

    @Transactional
    public QuizDto.AnswerResponse assess(UUID userId, UUID quizId, UUID questionId,
                                        QuizDto.SubmitAnswerRequest req) {
        Question q = questions.findByIdAndUserId(questionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        if (!q.getQuiz().getId().equals(quizId)) {
            throw new ResourceNotFoundException("Question not found in this quiz");
        }
        if (answers.findByQuestionIdAndUserId(questionId, userId).isPresent()) {
            throw new IllegalStateException("Question already answered");
        }

        AssessmentResult result;
        if ("MCQ".equalsIgnoreCase(q.getType())) {
            result = assessMcq(q, req);
        } else {
            result = ai.evaluateAnswer(new AiClient.AssessmentPrompt(
                    new AiClient.AiContext(userId, q.getProjectId(), "ASSESSMENT"),
                    q.getPrompt(), q.getType(), q.getCorrectAnswer(),
                    req.answerText(), q.getConcept() == null ? null : q.getConcept().getName()));
        }

        Answer a = new Answer();
        a.setQuestion(q);
        a.setQuiz(q.getQuiz());
        a.setUserId(userId);
        a.setProjectId(q.getProjectId());
        a.setAnswerText(req.answerText());
        a.setSelectedOption(req.selectedOption());
        a.setScore((double) result.score());
        a.setFeedback(result.feedback());
        try {
            a.setUnderstood(mapper.writeValueAsString(result.understood()));
            a.setMissing(mapper.writeValueAsString(result.missing()));
        } catch (Exception e) {
            a.setUnderstood("[]");
            a.setMissing("[]");
        }
        answers.save(a);

        if (q.getConcept() != null) {
            mastery.update(userId, q.getProjectId(), q.getConcept(), result.score());
            context.noteAssessment(userId, q.getProjectId(), q.getConcept().getName(),
                    result.score(), String.join("; ", result.missing()));
        }
        events.record(userId, q.getProjectId(), "QUESTION_ANSWERED",
                Map.of("quizId", quizId.toString(), "questionId", questionId.toString(),
                        "score", String.valueOf(result.score())),
                "answer-" + questionId + "-" + userId);

        return new QuizDto.AnswerResponse(a.getId().toString(), result.score(),
                result.feedback(), result.understood(), result.missing());
    }

    private AssessmentResult assessMcq(Question q, QuizDto.SubmitAnswerRequest req) {
        String given = req.selectedOption() == null ? "" : req.selectedOption().strip();
        // Fall back to answer text: some clients submit the option text instead of the letter.
        if (given.isEmpty() && req.answerText() != null && !req.answerText().isBlank()) {
            given = req.answerText().strip();
        }
        String expected = q.getCorrectAnswer() == null ? "" : q.getCorrectAnswer().strip();
        boolean correct = !given.isEmpty() && !expected.isEmpty()
                && (given.equalsIgnoreCase(expected)
                || stripOptionPrefix(given).equalsIgnoreCase(stripOptionPrefix(expected))
                || optionLetterMatches(q, given, expected)
                || optionLetterMatches(q, expected, given));
        return new AssessmentResult(correct ? 100 : 0,
                correct ? List.of("correct option selected") : List.of(),
                correct ? List.of() : List.of("correct option not selected"),
                correct ? "Correct — well done."
                        : "Not quite. Review the material on this concept and try a similar question.");
    }

    private boolean optionLetterMatches(Question q, String given, String expected) {
        try {
            if (given.length() != 1 || !Character.isLetter(given.charAt(0))) {
                return false;
            }
            int idx = Character.toUpperCase(given.charAt(0)) - 'A';
            List<String> opts = mapper.readValue(q.getOptions() == null ? "[]" : q.getOptions(),
                    mapper.getTypeFactory().constructCollectionType(List.class, String.class));
            // Compare prefix-insensitive so "A) Paris" options match a plain "Paris" answer and vice versa.
            String want = stripOptionPrefix(expected);
            return idx >= 0 && idx < opts.size()
                    && stripOptionPrefix(opts.get(idx)).equalsIgnoreCase(want);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Strips a leading option marker ("A) ", "B. ", "C: ", "(D) ", "Option A: ") for
     * comparison. A bare letter followed by a space ("A feature...") is NOT a marker.
     */
    static String stripOptionPrefix(String s) {
        if (s == null) {
            return "";
        }
        String t = s.strip();
        java.util.regex.Matcher paren =
                java.util.regex.Pattern.compile("^\\(([A-Za-z])\\)\\s*(.+)$").matcher(t);
        if (paren.matches()) {
            return paren.group(2).strip();
        }
        java.util.regex.Matcher opt =
                java.util.regex.Pattern.compile("(?i)^option\\s+([A-Za-z])\\s*[:.\\)\\-]\\s*(.+)$").matcher(t);
        if (opt.matches()) {
            return opt.group(2).strip();
        }
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("^([A-Za-z])\\s*[:.\\)\\-]\\s*(.+)$").matcher(t);
        if (m.matches()) {
            return m.group(2).strip();
        }
        return t;
    }

    /** Used by QuizService to resolve concept names from generated drafts. */
    Concept resolveConcept(UUID userId, UUID projectId, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String clean = name.strip();
        if (clean.length() > 300) {
            clean = clean.substring(0, 300);
        }
        final String finalName = clean;
        // Find-or-create: an AI-generated concept name missing from the DB must still
        // yield a Concept row, otherwise the question keeps a null concept and
        // per-answer mastery updates are silently skipped (empty Mastery page).
        return concepts.findByProjectIdAndUserIdAndNameIgnoreCase(projectId, userId, finalName)
                .orElseGet(() -> {
                    Project projectStub = new Project();
                    projectStub.setId(projectId);
                    User userStub = new User();
                    userStub.setId(userId);
                    Concept concept = new Concept();
                    concept.setProject(projectStub);
                    concept.setUser(userStub);
                    concept.setName(finalName);
                    return concepts.save(concept);
                });
    }
}
