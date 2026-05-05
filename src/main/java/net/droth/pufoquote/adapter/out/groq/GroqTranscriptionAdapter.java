package net.droth.pufoquote.adapter.out.groq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.droth.pufoquote.domain.model.Segment;
import net.droth.pufoquote.domain.port.out.TranscriptionPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

/** Transcription adapter that calls the Groq Whisper API. */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroqTranscriptionAdapter implements TranscriptionPort {

  private static final String GROQ_URL = "https://api.groq.com/openai/v1/audio/transcriptions";

  private final WebClient groqWebClient;

  @Override
  public List<Segment> transcribe(Path mp3Path) {
    log.info("Transcribing: {}", mp3Path.getFileName());

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", new FileSystemResource(mp3Path));
    body.add("model", "whisper-large-v3-turbo");
    body.add("response_format", "verbose_json");
    body.add("language", "de");
    body.add("temperature", "0");

    GroqResponse response =
        groqWebClient
            .post()
            .uri(GROQ_URL)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(body))
            .retrieve()
            .onStatus(
                HttpStatusCode::isError,
                res ->
                    res.bodyToMono(String.class)
                        .map(
                            b ->
                                new RuntimeException(
                                    "Groq API error " + res.statusCode() + ": " + b)))
            .bodyToMono(GroqResponse.class)
            .block();

    if (response == null || response.segments() == null) {
      log.warn("No segments returned from Groq for: {}", mp3Path.getFileName());
      return List.of();
    }

    return response.segments().stream()
        .map(s -> new Segment(s.start(), s.end(), s.text().trim()))
        .toList();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record GroqResponse(@JsonProperty("segments") List<GroqSegment> segments) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record GroqSegment(
      @JsonProperty("start") double start,
      @JsonProperty("end") double end,
      @JsonProperty("text") String text) {}
}
