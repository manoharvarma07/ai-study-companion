package com.aistudy.backend.ai;

import java.util.Locale;

/**
 * Tutor question intention. Drives retrieval breadth and offline answer
 * shaping: focused questions use semantic top-k, document-wide questions use
 * page-level document evidence. Pure heuristic — no AI call, fully
 * unit-testable. Lives in the {@code ai} package so both the retrieval layer
 * and the AI client can use it without a package cycle.
 */
public enum QueryIntent {
    DEFINITION,
    EXPLANATION,
    PROCESS,
    COMPARISON,
    ENUMERATION,
    NAVIGATION,
    SUMMARY,
    APPLIED,
    GENERAL;

    /** Pure heuristic classification — no AI call, fully unit-testable. */
    public static QueryIntent classify(String question) {
        String q = question == null ? "" : question.toLowerCase(Locale.ROOT).strip();
        if (q.isEmpty()) {
            return GENERAL;
        }
        if (q.contains("summar") || q.contains("tldr") || q.contains("brief overview")
                || q.contains("recap") || q.contains("overview of")) {
            return SUMMARY;
        }
        if (q.contains("which page") || q.contains("what page") || q.startsWith("where is")
                || q.startsWith("where are") || q.startsWith("where can") || q.contains("locate")
                || q.contains("find it in")) {
            return NAVIGATION;
        }
        if (q.contains("difference between") || q.contains(" vs ") || q.contains(" versus")
                || q.contains("compare") || q.contains("comparison")) {
            return COMPARISON;
        }
        if (q.contains("new student") || q.contains("new data point") || q.contains("new example")
                || q.contains("new data") || q.contains("predict") || q.contains("classify")
                || q.contains("calculate") || q.contains("compute") || q.contains("what class")
                || q.contains("which class") || q.contains("belongs to") || q.contains("what will")
                || q.contains("what would")) {
            return APPLIED;
        }
        if (q.contains("how many") || q.startsWith("list ") || q.startsWith("list all")
                || q.contains("enumerate") || q.contains("topics covered")
                || q.contains("topics are covered") || q.contains("covered in")
                || q.contains("which algorithms") || q.contains("which topics")
                || q.contains("which concepts") || q.contains("what topics")
                || q.contains("all algorithms") || q.contains("all topics")
                || q.contains("all concepts")) {
            return ENUMERATION;
        }
        if (q.startsWith("how does") || q.startsWith("how do") || q.startsWith("how is")
                || q.startsWith("how are") || q.contains("step by step") || q.contains("steps")) {
            return PROCESS;
        }
        if (q.startsWith("what is") || q.startsWith("what are") || q.startsWith("define")
                || q.contains("meaning of") || q.contains("explain what")) {
            return DEFINITION;
        }
        if (q.startsWith("explain") || q.startsWith("why") || q.startsWith("describe")
                || q.contains("explain") || q.contains("elaborate")) {
            return EXPLANATION;
        }
        return GENERAL;
    }
}
