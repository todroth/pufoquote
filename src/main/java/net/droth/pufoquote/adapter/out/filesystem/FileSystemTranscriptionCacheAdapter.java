package net.droth.pufoquote.adapter.out.filesystem;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.droth.pufoquote.domain.model.Segment;
import net.droth.pufoquote.domain.port.out.TranscriptionCachePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Transcription cache adapter that persists segment JSON on the local filesystem. */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileSystemTranscriptionCacheAdapter implements TranscriptionCachePort {

  private static final TypeReference<List<Segment>> SEGMENT_LIST_TYPE = new TypeReference<>() {};

  @Value("${transcription.cache-dir:./transcriptions}")
  private final Path cacheDir;

  private final ObjectMapper objectMapper;

  @PostConstruct
  void init() throws IOException {
    Files.createDirectories(cacheDir);
    log.info("Transcription cache directory: {}", cacheDir.toAbsolutePath());
  }

  @Override
  public Optional<List<Segment>> load(String episodeId) {
    Path file = cacheDir.resolve(sanitize(episodeId) + ".json");
    if (!Files.exists(file)) {
      return Optional.empty();
    }
    try {
      List<Segment> segments = objectMapper.readValue(file.toFile(), SEGMENT_LIST_TYPE);
      log.debug("Loaded {} segments from cache for episode {}", segments.size(), episodeId);
      return Optional.of(segments);
    } catch (JacksonException e) {
      log.warn("Corrupt cache file {}, ignoring: {}", file, e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public void save(String episodeId, List<Segment> segments) {
    Path file = cacheDir.resolve(sanitize(episodeId) + ".json");
    Path tmp = cacheDir.resolve(sanitize(episodeId) + ".json.tmp");
    try {
      objectMapper.writeValue(tmp.toFile(), segments);
      // Atomic move prevents a partially-written file from being read as a valid cache entry
      Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      log.debug("Cached {} segments for episode {}", segments.size(), episodeId);
    } catch (JacksonException | IOException e) {
      log.error("Failed to cache transcription for episode {}: {}", episodeId, e.getMessage(), e);
    }
  }

  // Replaces URL characters (slashes, colons, question marks) with dashes so the ID is a safe
  // filename
  private static String sanitize(String episodeId) {
    return episodeId.replaceAll("[^a-zA-Z0-9\\-]", "-");
  }
}
