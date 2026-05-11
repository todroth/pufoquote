package net.droth.pufoquote.domain.port.in;

import java.util.Optional;
import net.droth.pufoquote.domain.model.QuoteContext;

/** Use case for retrieving surrounding context sentences for a quote. */
public interface GetQuoteContextUseCase {
  Optional<QuoteContext> getContext(String quoteId);
}
