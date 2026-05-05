package net.droth.pufoquote.application;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import net.droth.pufoquote.domain.model.VoteResult;
import net.droth.pufoquote.domain.port.in.VoteForQuoteUseCase;
import net.droth.pufoquote.domain.port.out.VoteRepositoryPort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VoteForQuoteService implements VoteForQuoteUseCase {

  private final VoteRepositoryPort voteRepository;

  @Override
  public VoteResult vote(String quoteId, Set<String> alreadyVotedIds) {
    if (alreadyVotedIds.contains(quoteId)) {
      voteRepository.decrementVote(quoteId);
      return new VoteResult(false, voteRepository.getVoteCount(quoteId));
    }
    voteRepository.incrementVote(quoteId);
    return new VoteResult(true, voteRepository.getVoteCount(quoteId));
  }
}
