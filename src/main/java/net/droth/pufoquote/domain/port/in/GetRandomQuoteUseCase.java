package net.droth.pufoquote.domain.port.in;

import java.util.Optional;
import net.droth.pufoquote.domain.model.Category;
import net.droth.pufoquote.domain.model.Quote;

/** Use case for retrieving a random quote, optionally filtered by category. */
public interface GetRandomQuoteUseCase {
  Optional<Quote> getRandomQuote(Category category);
}
