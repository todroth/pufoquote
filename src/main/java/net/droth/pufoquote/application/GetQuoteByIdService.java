package net.droth.pufoquote.application;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import net.droth.pufoquote.domain.model.Quote;
import net.droth.pufoquote.domain.port.in.GetQuoteByIdUseCase;
import net.droth.pufoquote.domain.port.out.QuoteRepositoryPort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetQuoteByIdService implements GetQuoteByIdUseCase {

  private final QuoteRepositoryPort quoteRepository;

  @Override
  public Optional<Quote> getById(String id) {
    return quoteRepository.findById(id);
  }
}
