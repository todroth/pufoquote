package net.droth.pufoquote.domain.port.out;

import java.util.List;

/** Output port for storing and retrieving quote vote counts. */
public interface VoteRepositoryPort {
  void incrementVote(String quoteId);

  void decrementVote(String quoteId);

  long getVoteCount(String quoteId);

  List<String> getVotedQuoteIds(int offset, int limit);
}
