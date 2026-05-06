package net.droth.pufoquote.domain.port.out;

import java.util.List;
import java.util.Optional;
import net.droth.pufoquote.domain.model.CategorizedSentence;

/** Output port for loading and saving categorization cache entries. */
public interface CategorizationCachePort {
  Optional<List<CategorizedSentence>> load(String episodeId);

  void save(String episodeId, List<CategorizedSentence> sentences);
}
