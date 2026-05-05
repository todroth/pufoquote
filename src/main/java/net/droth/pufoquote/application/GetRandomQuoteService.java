package net.droth.pufoquote.application;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import net.droth.pufoquote.domain.model.Category;
import net.droth.pufoquote.domain.model.Quote;
import net.droth.pufoquote.domain.port.in.GetRandomQuoteUseCase;
import net.droth.pufoquote.domain.port.out.QuoteRepositoryPort;
import org.springframework.stereotype.Service;

/** Application service that retrieves a random quote from the repository. */
@Service
@RequiredArgsConstructor
public class GetRandomQuoteService implements GetRandomQuoteUseCase {

  private final QuoteRepositoryPort quoteRepository;

  @Override
  public Optional<Quote> getRandomQuote(Category category) {
    return quoteRepository.findRandom(category);
  }
}
