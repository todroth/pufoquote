package net.droth.pufoquote.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import net.droth.pufoquote.domain.model.EpisodeSummary;
import net.droth.pufoquote.domain.port.in.GetEpisodesUseCase;
import net.droth.pufoquote.domain.port.out.QuoteRepositoryPort;
import org.springframework.stereotype.Service;

/** Returns all indexed episodes sorted by date. */
@Service
@RequiredArgsConstructor
public class GetEpisodesService implements GetEpisodesUseCase {

  private final QuoteRepositoryPort quoteRepository;

  @Override
  public List<EpisodeSummary> getEpisodes() {
    return quoteRepository.findAllEpisodes();
  }
}
