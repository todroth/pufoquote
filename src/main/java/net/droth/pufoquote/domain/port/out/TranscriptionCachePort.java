package net.droth.pufoquote.domain.port.out;

import java.util.List;
import java.util.Optional;
import net.droth.pufoquote.domain.model.Segment;

/** Output port for loading and saving transcription cache entries. */
public interface TranscriptionCachePort {
  Optional<List<Segment>> load(String episodeId);

  void save(String episodeId, List<Segment> segments);
}
