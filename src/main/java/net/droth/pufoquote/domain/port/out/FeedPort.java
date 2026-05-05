package net.droth.pufoquote.domain.port.out;

import java.util.List;
import net.droth.pufoquote.domain.model.Episode;

/** Output port for fetching podcast episodes from a feed. */
public interface FeedPort {
  List<Episode> fetchEpisodes();
}
