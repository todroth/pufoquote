package net.droth.pufoquote.application;

import lombok.RequiredArgsConstructor;
import net.droth.pufoquote.domain.port.in.GetVoteCountUseCase;
import net.droth.pufoquote.domain.port.out.VoteRepositoryPort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetVoteCountService implements GetVoteCountUseCase {

  private final VoteRepositoryPort voteRepository;

  @Override
  public long getVoteCount(String quoteId) {
    return voteRepository.getVoteCount(quoteId);
  }
}
