package net.droth.pufoquote.adapter.out.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.droth.pufoquote.domain.model.CategorizedSentence;
import net.droth.pufoquote.domain.port.out.CategorizationCachePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Categorization cache adapter that persists classified sentences as JSON on the filesystem. */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileSystemCategorizationCacheAdapter implements CategorizationCachePort {

  private static final TypeReference<List<CategorizedSentence>> TYPE = new TypeReference<>() {};

  @Value("${transcription.cache-dir:./transcriptions}")
  private final Path cacheDir;

  private final ObjectMapper objectMapper;

  @Override
  public Optional<List<CategorizedSentence>> load(String episodeId) {
    Path file = cacheDir.resolve(sanitize(episodeId) + ".categorized.json");
    if (!Files.exists(file)) {
      return Optional.empty();
    }
    try {
      List<CategorizedSentence> sentences = objectMapper.readValue(file.toFile(), TYPE);
      log.debug(
          "Loaded {} categorized sentences from cache for episode {}", sentences.size(), episodeId);
      return Optional.of(sentences);
    } catch (JacksonException e) {
      log.warn("Corrupt categorization cache {}, ignoring: {}", file, e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public void save(String episodeId, List<CategorizedSentence> sentences) {
    Path file = cacheDir.resolve(sanitize(episodeId) + ".categorized.json");
    Path tmp = cacheDir.resolve(sanitize(episodeId) + ".categorized.json.tmp");
    try {
      objectMapper.writeValue(tmp.toFile(), sentences);
      Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      log.debug("Cached {} categorized sentences for episode {}", sentences.size(), episodeId);
    } catch (JacksonException | IOException e) {
      log.error("Failed to cache categorization for episode {}: {}", episodeId, e.getMessage(), e);
    }
  }

  private static String sanitize(String episodeId) {
    return episodeId.replaceAll("[^a-zA-Z0-9\\-]", "-");
  }
}
