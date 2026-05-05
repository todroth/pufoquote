package net.droth.pufoquote.domain.port.in;

/** Use case for triggering podcast episode indexing. */
public interface IndexEpisodesUseCase {
  void indexNewEpisodes();

  /** Drops all existing quotes and re-indexes every episode from the transcription cache. */
  void reindexAll();
}
