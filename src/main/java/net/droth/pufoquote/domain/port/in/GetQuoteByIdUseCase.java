package net.droth.pufoquote.domain.port.in;

import java.util.Optional;
import net.droth.pufoquote.domain.model.Quote;

public interface GetQuoteByIdUseCase {
  Optional<Quote> getById(String id);
}
