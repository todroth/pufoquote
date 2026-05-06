package net.droth.pufoquote.domain.port.in;

/** Use case for triggering podcast episode indexing. */
public interface IndexEpisodesUseCase {
  void indexNewEpisodes();

  /** Drops all existing quotes and re-indexes every episode from the transcription cache. */
  void reindexAll();

  /**
   * Reads all quotes currently in Elasticsearch and writes them to the categorization cache. Safe
   * to run at any time; skips episodes already cached. Run this once after restoring ES data from a
   * dump so that future reindexes are free and preserve quote IDs.
   */
  void seedCategorizationCache();
}
