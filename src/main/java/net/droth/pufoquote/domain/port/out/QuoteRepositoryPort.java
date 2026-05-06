package net.droth.pufoquote.domain.port.out;

import java.util.List;
import java.util.Optional;
import net.droth.pufoquote.domain.model.Category;
import net.droth.pufoquote.domain.model.Quote;
import net.droth.pufoquote.domain.model.QuoteContext;

/** Output port for storing and retrieving quotes from a persistent store. */
public interface QuoteRepositoryPort {
  void saveAll(String episodeId, List<Quote> quotes);

  boolean existsByEpisodeId(String episodeId);

  void deleteAll();

  Optional<Quote> findRandom(Category category);

  Optional<Quote> findById(String id);

  QuoteContext findContext(String episodeId, double startSeconds, int count);

  List<Quote> findAllByEpisodeId(String episodeId);
}
