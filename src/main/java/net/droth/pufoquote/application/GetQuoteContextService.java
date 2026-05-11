package net.droth.pufoquote.application;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import net.droth.pufoquote.domain.model.QuoteContext;
import net.droth.pufoquote.domain.port.in.GetQuoteContextUseCase;
import net.droth.pufoquote.domain.port.out.QuoteRepositoryPort;
import org.springframework.stereotype.Service;

/** Retrieves the sentences immediately before and after a given quote in the same episode. */
@Service
@RequiredArgsConstructor
public class GetQuoteContextService implements GetQuoteContextUseCase {

  private final QuoteRepositoryPort quoteRepository;

  @Override
  public Optional<QuoteContext> getContext(String quoteId) {
    return quoteRepository
        .findById(quoteId)
        .map(quote -> quoteRepository.findContext(quote.episodeId(), quote.startSeconds(), 2));
  }
}
