package net.droth.pufoquote.adapter.in.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.droth.pufoquote.domain.port.in.IndexEpisodesUseCase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for admin endpoints that trigger episode indexing. */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

  private final IndexEpisodesUseCase indexEpisodesUseCase;

  @Qualifier("applicationTaskExecutor")
  private final TaskExecutor taskExecutor;

  /** Triggers asynchronous indexing of all new podcast episodes. */
  @PostMapping("/index-all")
  public ResponseEntity<String> indexAll() {
    log.info("Manual index triggered via /admin/index-all");
    taskExecutor.execute(
        () -> {
          try {
            indexEpisodesUseCase.indexNewEpisodes();
          } catch (Exception e) {
            log.error("Indexing failed", e);
          }
        });
    return ResponseEntity.accepted().body("Indexing started. Check application logs for progress.");
  }

  /**
   * Reads all quotes currently in Elasticsearch and writes them to the categorization cache. Run
   * this once after restoring an ES dump so that future reindexes are free and preserve quote IDs.
   */
  @PostMapping("/seed-categorization-cache")
  public ResponseEntity<String> seedCategorizationCache() {
    log.info("Categorization cache seed triggered via /admin/seed-categorization-cache");
    taskExecutor.execute(
        () -> {
          try {
            indexEpisodesUseCase.seedCategorizationCache();
          } catch (Exception e) {
            log.error("Cache seeding failed", e);
          }
        });
    return ResponseEntity.accepted()
        .body("Cache seeding started. Check application logs for progress.");
  }

  /** Drops all existing quotes and re-indexes everything from the transcription cache. */
  @PostMapping("/reindex-all")
  public ResponseEntity<String> reindexAll() {
    log.info("Full reindex triggered via /admin/reindex-all");
    taskExecutor.execute(
        () -> {
          try {
            indexEpisodesUseCase.reindexAll();
          } catch (Exception e) {
            log.error("Reindexing failed", e);
          }
        });
    return ResponseEntity.accepted()
        .body("Full reindex started. Check application logs for progress.");
  }
}
