package net.droth.pufoquote.adapter.in.web.dto;

/** View model for a quote, used in Thymeleaf templates and JSON API responses. */
public record QuoteViewModel(
    String id,
    String text,
    String episodeName,
    String episodeDate,
    String timestamp,
    String episodeUrl,
    long voteCount,
    boolean alreadyVoted,
    String category) {}
