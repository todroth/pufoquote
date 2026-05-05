package net.droth.pufoquote.adapter.in.web.dto;

public record QuoteViewModel(
    String id,
    String text,
    String episodeName,
    String episodeDate,
    String timestamp,
    String episodeUrl,
    long voteCount,
    boolean alreadyVoted) {}
