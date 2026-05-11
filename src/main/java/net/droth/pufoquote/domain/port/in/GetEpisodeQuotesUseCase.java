package net.droth.pufoquote.domain.port.in;

import java.util.List;
import net.droth.pufoquote.domain.model.Quote;

/** Use case for retrieving the high-quality quotes for a specific episode. */
public interface GetEpisodeQuotesUseCase {
  List<Quote> getQuotes(String episodeId);
}
