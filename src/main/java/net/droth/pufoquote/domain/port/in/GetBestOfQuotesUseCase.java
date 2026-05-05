package net.droth.pufoquote.domain.port.in;

import java.util.List;
import net.droth.pufoquote.domain.model.BestOfQuote;

public interface GetBestOfQuotesUseCase {
  List<BestOfQuote> getQuotes(int offset, int limit);
}
