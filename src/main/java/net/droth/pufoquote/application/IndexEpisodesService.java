package net.droth.pufoquote.application;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.droth.pufoquote.domain.model.CategorizedSentence;
import net.droth.pufoquote.domain.model.Category;
import net.droth.pufoquote.domain.model.Classification;
import net.droth.pufoquote.domain.model.Episode;
import net.droth.pufoquote.domain.model.Quote;
import net.droth.pufoquote.domain.model.Segment;
import net.droth.pufoquote.domain.model.SentenceWithTimestamp;
import net.droth.pufoquote.domain.port.in.IndexEpisodesUseCase;
import net.droth.pufoquote.domain.port.out.CategorizationCachePort;
import net.droth.pufoquote.domain.port.out.CategorizationPort;
import net.droth.pufoquote.domain.port.out.FeedPort;
import net.droth.pufoquote.domain.port.out.QuoteRepositoryPort;
import net.droth.pufoquote.domain.port.out.TranscriptionCachePort;
import net.droth.pufoquote.domain.port.out.TranscriptionPort;
import net.droth.pufoquote.domain.service.SentenceSplitter;
import org.springframework.stereotype.Service;

/** Application service that orchestrates the full episode indexing pipeline. */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexEpisodesService implements IndexEpisodesUseCase {

  private static final int CATEGORIZATION_BATCH_SIZE = 30;
  private static final int CONTEXT_WINDOW = 2;

  private final AtomicBoolean indexingInProgress = new AtomicBoolean(false);

  private final FeedPort feedPort;
  private final TranscriptionPort transcriptionPort;
  private final TranscriptionCachePort transcriptionCachePort;
  private final QuoteRepositoryPort quoteRepository;
  private final CategorizationPort categorizationPort;
  private final CategorizationCachePort categorizationCache;
  private final SentenceSplitter sentenceSplitter;

  @Override
  public void indexNewEpisodes() {
    if (!indexingInProgress.compareAndSet(false, true)) {
      log.warn("Indexing already in progress — skipping concurrent request.");
      return;
    }
    try {
      doIndex(false);
    } finally {
      indexingInProgress.set(false);
    }
  }

  @Override
  public void reindexAll() {
    if (!indexingInProgress.compareAndSet(false, true)) {
      log.warn("Indexing already in progress — skipping concurrent request.");
      return;
    }
    try {
      log.info("Reindex-all: dropping existing quotes index");
      quoteRepository.deleteAll();
      doIndex(true);
    } finally {
      indexingInProgress.set(false);
    }
  }

  @Override
  public void seedCategorizationCache() {
    List<Episode> episodes = feedPort.fetchEpisodes();
    int seeded = 0;
    for (Episode episode : episodes) {
      if (categorizationCache.load(episode.id()).isPresent()) {
        continue;
      }
      List<Quote> quotes = quoteRepository.findAllByEpisodeId(episode.id());
      if (quotes.isEmpty()) {
        continue;
      }
      List<CategorizedSentence> sentences =
          quotes.stream()
              .map(
                  q ->
                      new CategorizedSentence(
                          q.id(),
                          q.text(),
                          q.startSeconds(),
                          q.categories().isEmpty() ? Category.NONE : q.categories().get(0),
                          q.qualityScore()))
              .toList();
      categorizationCache.save(episode.id(), sentences);
      seeded++;
    }
    log.info("Seeded categorization cache for {} episodes.", seeded);
  }

  private void doIndex(boolean force) {
    List<Episode> episodes = feedPort.fetchEpisodes();
    log.info("Fetched {} episodes from feed", episodes.size());

    int indexed = 0;
    for (Episode episode : episodes) {
      if (!force && quoteRepository.existsByEpisodeId(episode.id())) {
        log.debug("Already indexed, skipping: {}", episode.title());
        continue;
      }

      log.info("Indexing: {}", episode.title());
      Path downloaded = null;
      Path compressed = null;
      try {
        Optional<List<Segment>> cached = transcriptionCachePort.load(episode.id());
        List<Segment> segments;
        if (cached.isPresent()) {
          segments = cached.get();
          log.info(
              "Using cached transcription ({} segments) for: {}", segments.size(), episode.title());
        } else {
          downloaded = downloadMp3(episode.mp3Url());
          compressed = compress(downloaded);
          segments = transcriptionPort.transcribe(compressed);
          transcriptionCachePort.save(episode.id(), segments);
        }

        List<Quote> quotes = extractAndCategorize(episode, segments);
        if (!quotes.isEmpty()) {
          quoteRepository.saveAll(episode.id(), quotes);
          indexed++;
          log.info("Indexed {} quotes for: {}", quotes.size(), episode.title());
        } else {
          log.warn("No quotes extracted for: {}", episode.title());
        }
      } catch (Exception e) {
        log.error("Failed to index episode '{}': {}", episode.title(), e.getMessage(), e);
      } finally {
        deleteSilently(downloaded);
        deleteSilently(compressed);
      }
    }
    log.info("Indexing complete — {} new episodes indexed.", indexed);
  }

  private List<Quote> extractAndCategorize(Episode episode, List<Segment> segments) {
    Optional<List<CategorizedSentence>> cached = categorizationCache.load(episode.id());
    if (cached.isPresent()) {
      log.info("Using cached categorization for: {}", episode.title());
      return toQuotes(episode, cached.get());
    }

    List<SentenceWithTimestamp> sentences = sentenceSplitter.split(segments);
    log.debug("Extracted {} sentences from {} segments", sentences.size(), segments.size());

    List<CategorizedSentence> categorized = new ArrayList<>();

    for (int i = 0; i < sentences.size(); i += CATEGORIZATION_BATCH_SIZE) {
      List<SentenceWithTimestamp> batch =
          sentences.subList(i, Math.min(i + CATEGORIZATION_BATCH_SIZE, sentences.size()));
      List<String> texts = batch.stream().map(SentenceWithTimestamp::text).toList();
      List<String> ctxBefore =
          sentences.subList(Math.max(0, i - CONTEXT_WINDOW), i).stream()
              .map(SentenceWithTimestamp::text)
              .toList();
      List<String> ctxAfter =
          sentences
              .subList(
                  Math.min(i + CATEGORIZATION_BATCH_SIZE, sentences.size()),
                  Math.min(i + CATEGORIZATION_BATCH_SIZE + CONTEXT_WINDOW, sentences.size()))
              .stream()
              .map(SentenceWithTimestamp::text)
              .toList();
      List<Classification> classifications =
          categorizationPort.classify(texts, ctxBefore, ctxAfter);

      for (int j = 0; j < batch.size(); j++) {
        Classification classification =
            j < classifications.size()
                ? classifications.get(j)
                : new Classification(Category.NONE, 1);
        SentenceWithTimestamp sentence = batch.get(j);
        categorized.add(
            new CategorizedSentence(
                UUID.randomUUID().toString(),
                sentence.text(),
                sentence.startSeconds(),
                classification.category(),
                classification.qualityScore()));
      }
    }

    categorizationCache.save(episode.id(), categorized);
    return toQuotes(episode, categorized);
  }

  private List<Quote> toQuotes(Episode episode, List<CategorizedSentence> categorized) {
    return categorized.stream()
        .map(
            cs -> {
              int wordCount = cs.text().trim().split("\\s+").length;
              return new Quote(
                  cs.id(),
                  episode.id(),
                  episode.title(),
                  episode.date() != null ? episode.date().toString() : "",
                  episode.episodeUrl(),
                  episode.mp3Url(),
                  cs.startSeconds(),
                  cs.text(),
                  wordCount,
                  cs.qualityScore(),
                  List.of(cs.category()));
            })
        .toList();
  }

  private Path downloadMp3(String url) throws IOException {
    Path tempFile = Files.createTempFile("podcast-", ".mp3");
    try (InputStream in = URI.create(url).toURL().openStream()) {
      Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
    }
    log.debug(
        "Downloaded {} MB from {}", String.format("%.1f", Files.size(tempFile) / 1_000_000.0), url);
    return tempFile;
  }

  private Path compress(Path input) throws IOException, InterruptedException {
    Path output = Files.createTempFile("podcast-compressed-", ".mp3");
    // Re-encode to mono 32 kbps — keeps file under Groq's 25 MB upload limit
    Process process =
        new ProcessBuilder(
                "ffmpeg",
                "-i",
                input.toString(),
                "-ac",
                "1",
                "-ar",
                "16000",
                "-b:a",
                "32k",
                "-y",
                output.toString())
            .inheritIO()
            .start();
    boolean finished = process.waitFor(5, TimeUnit.MINUTES);
    if (!finished) {
      process.destroyForcibly();
      throw new IOException("ffmpeg timed out for: " + input.getFileName());
    }
    if (process.exitValue() != 0) {
      throw new IOException(
          "ffmpeg exited with code " + process.exitValue() + " for: " + input.getFileName());
    }
    log.debug(
        "Compressed {} → {} MB",
        input.getFileName(),
        String.format("%.1f", Files.size(output) / 1_000_000.0));
    return output;
  }

  private void deleteSilently(Path path) {
    if (path == null) {
      return;
    }
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      log.warn("Could not delete temp file {}: {}", path, e.getMessage());
    }
  }
}
