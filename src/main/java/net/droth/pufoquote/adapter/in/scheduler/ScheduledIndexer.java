package net.droth.pufoquote.adapter.in.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.droth.pufoquote.domain.port.in.IndexEpisodesUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Scheduled task that triggers daily episode indexing. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledIndexer {

  private final IndexEpisodesUseCase indexEpisodesUseCase;

  /** Runs the episode indexing job daily at 06:00 UTC. */
  @Scheduled(cron = "0 0 6 * * *")
  public void scheduledIndex() {
    log.info("Running scheduled episode indexing");
    try {
      indexEpisodesUseCase.indexNewEpisodes();
    } catch (Exception e) {
      log.error("Scheduled indexing failed", e);
    }
  }
}
