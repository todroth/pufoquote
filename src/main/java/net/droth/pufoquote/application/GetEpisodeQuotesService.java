package net.droth.pufoquote.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import net.droth.pufoquote.domain.model.Quote;
import net.droth.pufoquote.domain.port.in.GetEpisodeQuotesUseCase;
import net.droth.pufoquote.domain.port.out.QuoteRepositoryPort;
import org.springframework.stereotype.Service;

/** Returns score-4 and score-5 quotes for an episode. */
@Service
@RequiredArgsConstructor
public class GetEpisodeQuotesService implements GetEpisodeQuotesUseCase {

  private final QuoteRepositoryPort quoteRepository;

  @Override
  public List<Quote> getQuotes(String episodeId) {
    return quoteRepository.findAllByEpisodeId(episodeId).stream()
        .filter(q -> q.qualityScore() >= 4)
        .toList();
  }
}
