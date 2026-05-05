package net.droth.pufoquote.domain.port.in;

public interface GetVoteCountUseCase {
  long getVoteCount(String quoteId);
}
