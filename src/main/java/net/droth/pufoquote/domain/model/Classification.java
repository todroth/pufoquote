package net.droth.pufoquote.domain.model;

/** The result of classifying a single sentence: a category and a quality score 1–5. */
public record Classification(Category category, int qualityScore) {}
