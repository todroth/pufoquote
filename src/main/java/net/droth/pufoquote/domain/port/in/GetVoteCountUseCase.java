package net.droth.pufoquote.domain.port.in;

/** Use case for retrieving the current vote count of a quote. */
public interface GetVoteCountUseCase {
  long getVoteCount(String quoteId);
}
