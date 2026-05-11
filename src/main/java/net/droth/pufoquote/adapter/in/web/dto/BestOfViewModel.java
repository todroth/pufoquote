package net.droth.pufoquote.adapter.in.web.dto;

/** View model for a best-of quote with its vote count. */
public record BestOfViewModel(QuoteViewModel quote, long voteCount) {}
