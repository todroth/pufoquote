package net.droth.pufoquote.domain.model;

/** A sentence extracted from audio blocks, with the start time of the block it began in. */
public record SentenceWithTimestamp(String text, double startSeconds) {}
