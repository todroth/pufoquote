package net.droth.pufoquote.domain.model;

/** Result of a vote operation: whether the vote was accepted and the new vote count. */
public record VoteResult(boolean accepted, long voteCount) {}
