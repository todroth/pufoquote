package net.droth.pufoquote.domain.port.in;

import java.util.List;
import net.droth.pufoquote.domain.model.EpisodeSummary;

/** Use case for retrieving all indexed episodes. */
public interface GetEpisodesUseCase {
  List<EpisodeSummary> getEpisodes();
}
