package net.droth.pufoquote.domain.port.in;

import java.util.Optional;
import net.droth.pufoquote.domain.model.QuoteContext;

public interface GetQuoteContextUseCase {
  Optional<QuoteContext> getContext(String quoteId);
}
