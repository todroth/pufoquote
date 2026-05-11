package net.droth.pufoquote.domain.model;

/** A quote paired with its vote count, used for the best-of ranking. */
public record BestOfQuote(Quote quote, long voteCount) {}
