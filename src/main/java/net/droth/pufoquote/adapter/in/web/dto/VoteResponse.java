package net.droth.pufoquote.adapter.in.web.dto;

/** JSON response body for the vote endpoint. */
public record VoteResponse(long voteCount, boolean alreadyVoted) {}
