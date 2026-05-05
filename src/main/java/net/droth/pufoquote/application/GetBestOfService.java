package net.droth.pufoquote.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import net.droth.pufoquote.domain.model.BestOfQuote;
import net.droth.pufoquote.domain.port.in.GetBestOfQuotesUseCase;
import net.droth.pufoquote.domain.port.out.QuoteRepositoryPort;
import net.droth.pufoquote.domain.port.out.VoteRepositoryPort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetBestOfService implements GetBestOfQuotesUseCase {

  private final VoteRepositoryPort voteRepository;
  private final QuoteRepositoryPort quoteRepository;

  @Override
  public List<BestOfQuote> getTopQuotes(int limit) {
    return voteRepository.getTopVotedQuoteIds(limit).stream()
        .flatMap(
            id ->
                quoteRepository
                    .findById(id)
                    .map(q -> new BestOfQuote(q, voteRepository.getVoteCount(id)))
                    .stream())
        .toList();
  }
}
