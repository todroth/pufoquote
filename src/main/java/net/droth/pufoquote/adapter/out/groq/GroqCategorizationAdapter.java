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
      " Find sentences that are genuine gems — truly funny, surprising, or quotable."
          + " You MUST label at least 27 out of every 30 sentences as 'none'."
          + " Assign a non-none label ONLY when a sentence would make a stranger"
          + " laugh, think, or want to listen to the episode. When in doubt: none.\n\n"
          + "Labels — be precise, use exactly one:\n"
          + "  funny      — genuinely funny; makes you laugh out loud\n"
          + "  absurd     — bizarre or surreal in a striking way\n"
          + "  interesting — surprising fact or unexpected insight, not just mildly interesting\n"
          + "  philosophical — meaningfully thought-provoking, not just vague reflection\n"
          + "  dramatic   — hilariously over-the-top about something trivial\n"
          + "  self_aware — clever meta-commentary about the podcast itself\n"
          + "  none       — everything else: filler, transitions, small talk, mundane facts,"
          + " incomplete thoughts, garbled transcription\n\n"
          + "Score 1-5 — be strict:\n"
          + "  1 = garbled, incoherent, or meaningless\n"
          + "  2 = boring or unremarkable\n"
          + "  3 = decent standalone quote\n"
          + "  4 = memorable — would make someone smile or think\n"
          + "  5 = exceptional — would make someone want to listen to the episode\n\n"
          + "RULE: If a sentence gets a non-none label it must score at least 3."
          + " Most non-none sentences should score 3-4; score 5 is rare.\n\n";

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
