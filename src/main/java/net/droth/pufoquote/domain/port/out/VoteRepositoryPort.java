package net.droth.pufoquote.domain.port.out;

import java.util.List;

public interface VoteRepositoryPort {
  void incrementVote(String quoteId);

  void decrementVote(String quoteId);

  long getVoteCount(String quoteId);

  List<String> getTopVotedQuoteIds(int limit);
}
