package net.droth.pufoquote.adapter.out.groq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.droth.pufoquote.domain.model.Category;
import net.droth.pufoquote.domain.model.Classification;
import net.droth.pufoquote.domain.port.out.CategorizationPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Categorization adapter that uses the Groq chat completion API to label and score sentences. */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroqCategorizationAdapter implements CategorizationPort {

  private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

  // Each element is "label:score", e.g. "funny:4" or "none:1"
  private static final String PROMPT_BODY =
      " Your job is to find the rare gems — sentences that are genuinely funny, surprising,"
          + " quotable, or insightful. Be very selective: MOST sentences (>90%) should be"
          + " labeled 'none'.\n\n"
          + "Label (assign non-none only if the sentence truly stands out):\n"
          + "  funny      — actually makes you laugh or smile\n"
          + "  absurd     — genuinely bizarre or surreal\n"
          + "  interesting — surprising fact or unexpected insight\n"
          + "  philosophical — meaningfully reflective or thought-provoking\n"
          + "  dramatic   — hilariously over-the-top about something trivial\n"
          + "  self_aware — clever meta-commentary about the podcast itself\n"
          + "  none       — everything else (filler, transitions, mundane conversation,"
          + " incomplete fragments, garbled transcription)\n\n"
          + "Score 1-5 (be strict — most sentences score 1-2):\n"
          + "  1 = garbled/incoherent/meaningless\n"
          + "  2 = boring or unremarkable\n"
          + "  3 = genuinely worth reading as a standalone quote\n"
          + "  4 = memorable, would make someone smile or think\n"
          + "  5 = exceptional — would make someone want to listen to the episode\n\n";

  @Value("${groq.categorization-model:llama-3.1-8b-instant}")
  private final String model;

  private final WebClient groqWebClient;
  private final ObjectMapper objectMapper;

  @Override
  public List<Classification> classify(List<String> sentences) {
    if (sentences.isEmpty()) {
      return List.of();
    }

    String prompt = buildPrompt(sentences);
    String responseContent = callGroq(prompt, sentences.size());
    return parseClassifications(responseContent, sentences.size());
  }

  private String buildPrompt(List<String> sentences) {
    int n = sentences.size();
    StringBuilder sb = new StringBuilder();
    sb.append("You receive ").append(n).append(" German sentences from a podcast transcript.");
    sb.append(PROMPT_BODY);
    sb.append("Output ONLY a JSON array of ")
        .append(n)
        .append(" strings like [\"funny:4\",\"none:1\",...]. No other text.\n\n");
    for (int i = 0; i < n; i++) {
      sb.append(i + 1).append(". ").append(sentences.get(i)).append('\n');
    }
    return sb.toString();
  }

  private String callGroq(String prompt, int batchSize) {
    Map<String, Object> requestBody =
        Map.of(
            "model",
            model,
            "messages",
            List.of(Map.of("role", "user", "content", prompt)),
            "temperature",
            0.1,
            "max_tokens",
            1200);

    GroqChatResponse response =
        groqWebClient
            .post()
            .uri(GROQ_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .onStatus(
                HttpStatusCode::isError,
                res ->
                    res.bodyToMono(String.class)
                        .map(
                            b ->
                                new RuntimeException(
                                    "Groq categorization error " + res.statusCode() + ": " + b)))
            .bodyToMono(GroqChatResponse.class)
            .block();

    if (response == null
        || response.choices() == null
        || response.choices().isEmpty()
        || response.choices().get(0).message() == null) {
      log.warn("Empty response from Groq categorization for batch of {}", batchSize);
      return "[]";
    }
    return response.choices().get(0).message().content();
  }

  private List<Classification> parseClassifications(String json, int expectedSize) {
    try {
      String cleaned = json.trim();
      if (cleaned.startsWith("```")) {
        cleaned = cleaned.replaceAll("^```[a-z]*\\n?", "").replaceAll("\\n?```$", "").trim();
      }
      List<String> labels = objectMapper.readValue(cleaned, STRING_LIST);
      List<Classification> result = new ArrayList<>(expectedSize);
      for (String label : labels) {
        result.add(parseLabel(label));
      }
      while (result.size() < expectedSize) {
        result.add(new Classification(Category.NONE, 1));
      }
      return result;
    } catch (Exception e) {
      log.warn("Failed to parse categorization response: {} — defaulting to NONE", e.getMessage());
      return noneList(expectedSize);
    }
  }

  // Parses "funny:4" → Classification(FUNNY, 4); falls back to NONE/1 on any malformed input
  private static Classification parseLabel(String label) {
    if (label == null || label.isBlank()) {
      return new Classification(Category.NONE, 1);
    }
    String[] parts = label.trim().split(":", 2);
    Category category = labelToCategory(parts[0]);
    int score = 1;
    if (parts.length == 2) {
      try {
        score = Math.max(1, Math.min(5, Integer.parseInt(parts[1].trim())));
      } catch (NumberFormatException ignored) {
        score = category == Category.NONE ? 1 : 3;
      }
    }
    return new Classification(category, score);
  }

  private static Category labelToCategory(String label) {
    if (label == null) {
      return Category.NONE;
    }
    return switch (label.toLowerCase().trim()) {
      case "funny" -> Category.FUNNY;
      case "absurd" -> Category.ABSURD;
      case "interesting" -> Category.INTERESTING;
      case "philosophical" -> Category.PHILOSOPHICAL;
      case "dramatic" -> Category.DRAMATIC;
      case "self_aware" -> Category.SELF_AWARE;
      default -> Category.NONE;
    };
  }

  private static List<Classification> noneList(int size) {
    List<Classification> result = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      result.add(new Classification(Category.NONE, 1));
    }
    return result;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record GroqChatResponse(@JsonProperty("choices") List<Choice> choices) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Choice(@JsonProperty("message") Message message) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Message(@JsonProperty("content") String content) {}
}
