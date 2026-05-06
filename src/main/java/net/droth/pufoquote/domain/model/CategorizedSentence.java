package net.droth.pufoquote.domain.model;

/**
 * A categorized sentence with a stable ID — cached to disk so reindexing reuses the same quote IDs
 * and Redis vote counts remain valid.
 */
public record CategorizedSentence(
    String id, String text, double startSeconds, Category category, int qualityScore) {}
