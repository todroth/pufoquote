package net.droth.pufoquote.domain.port.in;

import java.util.Optional;
import net.droth.pufoquote.domain.model.Quote;

/** Use case for retrieving a quote by its ID. */
public interface GetQuoteByIdUseCase {
  Optional<Quote> getById(String id);
}
