package net.droth.pufoquote.adapter.in.web.dto;

/** View model for an episode quote with its quality score. */
public record EpisodeQuoteViewModel(QuoteViewModel quote, int qualityScore) {}
