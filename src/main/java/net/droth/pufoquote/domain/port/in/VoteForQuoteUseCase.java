package net.droth.pufoquote.domain.port.in;

import java.util.Set;
import net.droth.pufoquote.domain.model.VoteResult;

public interface VoteForQuoteUseCase {
  VoteResult vote(String quoteId, Set<String> alreadyVotedIds);
}
