package net.droth.pufoquote.adapter.in.web;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.droth.pufoquote.adapter.in.web.dto.BestOfViewModel;
import net.droth.pufoquote.adapter.in.web.dto.QuoteViewModel;
import net.droth.pufoquote.adapter.in.web.dto.VoteResponse;
import net.droth.pufoquote.domain.model.Category;
import net.droth.pufoquote.domain.model.Quote;
import net.droth.pufoquote.domain.model.QuoteContext;
import net.droth.pufoquote.domain.model.VoteResult;
import net.droth.pufoquote.domain.port.in.GetBestOfQuotesUseCase;
import net.droth.pufoquote.domain.port.in.GetQuoteByIdUseCase;
import net.droth.pufoquote.domain.port.in.GetQuoteContextUseCase;
import net.droth.pufoquote.domain.port.in.GetRandomQuoteUseCase;
import net.droth.pufoquote.domain.port.in.GetVoteCountUseCase;
import net.droth.pufoquote.domain.port.in.VoteForQuoteUseCase;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class QuoteController {

  private static final String COOKIE_NAME = "voted_quotes";
  private static final long COOKIE_MAX_AGE = 365 * 24 * 3600L;

  private final GetRandomQuoteUseCase getRandomQuoteUseCase;
  private final GetQuoteByIdUseCase getQuoteByIdUseCase;
  private final GetQuoteContextUseCase getQuoteContextUseCase;
  private final GetVoteCountUseCase getVoteCountUseCase;
  private final VoteForQuoteUseCase voteForQuoteUseCase;
  private final GetBestOfQuotesUseCase getBestOfQuotesUseCase;

  @GetMapping("/")
  public String index(
      @RequestParam(name = "category", defaultValue = "RANDOM") String categoryParam,
      @CookieValue(name = COOKIE_NAME, defaultValue = "") String votedCookie,
      Model model) {
    Category category = Category.fromString(categoryParam);
    Set<String> voted = parseCookie(votedCookie);
    getRandomQuoteUseCase
        .getRandomQuote(category)
        .map(q -> toViewModel(q, voted))
        .ifPresent(vm -> model.addAttribute("quote", vm));
    model.addAttribute("categories", Category.uiValues());
    model.addAttribute("currentCategory", category);
    return "index";
  }

  @GetMapping("/quote/{id}")
  public String quoteById(
      @PathVariable String id,
      @CookieValue(name = COOKIE_NAME, defaultValue = "") String votedCookie,
      Model model) {
    Set<String> voted = parseCookie(votedCookie);
    getQuoteByIdUseCase
        .getById(id)
        .map(q -> toViewModel(q, voted))
        .ifPresent(vm -> model.addAttribute("quote", vm));
    model.addAttribute("categories", Category.uiValues());
    model.addAttribute("currentCategory", Category.RANDOM);
    return "index";
  }

  @GetMapping(value = "/api/quote/{id}")
  @ResponseBody
  public ResponseEntity<QuoteViewModel> apiQuoteById(
      @PathVariable String id,
      @CookieValue(name = COOKIE_NAME, defaultValue = "") String votedCookie) {
    Set<String> voted = parseCookie(votedCookie);
    return getQuoteByIdUseCase
        .getById(id)
        .map(q -> toViewModel(q, voted))
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping(value = "/api/quote")
  @ResponseBody
  public ResponseEntity<QuoteViewModel> apiQuote(
      @RequestParam(name = "category", defaultValue = "RANDOM") String categoryParam,
      @CookieValue(name = COOKIE_NAME, defaultValue = "") String votedCookie) {
    Category category = Category.fromString(categoryParam);
    Set<String> voted = parseCookie(votedCookie);
    return getRandomQuoteUseCase
        .getRandomQuote(category)
        .map(q -> toViewModel(q, voted))
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.noContent().build());
  }

  @GetMapping(value = "/api/quote/{id}/context")
  @ResponseBody
  public ResponseEntity<QuoteContext> apiContext(@PathVariable String id) {
    return getQuoteContextUseCase
        .getContext(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping(value = "/api/quote/{id}/vote")
  @ResponseBody
  public ResponseEntity<VoteResponse> vote(
      @PathVariable String id,
      @CookieValue(name = COOKIE_NAME, defaultValue = "") String votedCookie,
      HttpServletResponse response) {
    Set<String> voted = parseCookie(votedCookie);
    VoteResult result = voteForQuoteUseCase.vote(id, voted);
    Set<String> updated = new HashSet<>(voted);
    if (result.accepted()) {
      updated.add(id);
    } else {
      updated.remove(id);
    }
    response.addHeader(HttpHeaders.SET_COOKIE, buildSetCookie(updated));
    return ResponseEntity.ok(new VoteResponse(result.voteCount(), result.accepted()));
  }

  @GetMapping("/best")
  public String best(
      @CookieValue(name = COOKIE_NAME, defaultValue = "") String votedCookie, Model model) {
    Set<String> voted = parseCookie(votedCookie);
    List<BestOfViewModel> items =
        getBestOfQuotesUseCase.getQuotes(0, 20).stream()
            .map(b -> new BestOfViewModel(toViewModel(b.quote(), voted), b.voteCount()))
            .toList();
    model.addAttribute("items", items);
    model.addAttribute("categories", Category.uiValues());
    model.addAttribute("currentCategory", null);
    return "best";
  }

  @GetMapping(value = "/api/best")
  @ResponseBody
  public List<BestOfViewModel> apiBest(
      @RequestParam(name = "offset", defaultValue = "0") int offset,
      @RequestParam(name = "limit", defaultValue = "20") int limit,
      @CookieValue(name = COOKIE_NAME, defaultValue = "") String votedCookie) {
    Set<String> voted = parseCookie(votedCookie);
    return getBestOfQuotesUseCase.getQuotes(offset, limit).stream()
        .map(b -> new BestOfViewModel(toViewModel(b.quote(), voted), b.voteCount()))
        .toList();
  }

  private QuoteViewModel toViewModel(Quote quote, Set<String> votedIds) {
    return new QuoteViewModel(
        quote.id(),
        quote.text(),
        quote.episodeName(),
        quote.episodeDate(),
        formatTimestamp(quote.startSeconds()),
        quote.episodeUrl(),
        getVoteCountUseCase.getVoteCount(quote.id()),
        votedIds.contains(quote.id()));
  }

  private static Set<String> parseCookie(String cookieValue) {
    if (cookieValue == null || cookieValue.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(cookieValue.split("\\|"))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .collect(Collectors.toUnmodifiableSet());
  }

  private static String buildSetCookie(Set<String> ids) {
    List<String> capped = ids.stream().limit(100).toList();
    return ResponseCookie.from(COOKIE_NAME, String.join("|", capped))
        .httpOnly(true)
        .sameSite("Strict")
        .maxAge(COOKIE_MAX_AGE)
        .path("/")
        .build()
        .toString();
  }

  private static String formatTimestamp(double totalSeconds) {
    int s = (int) totalSeconds;
    int hours = s / 3600;
    int minutes = (s % 3600) / 60;
    int seconds = s % 60;
    if (hours > 0) {
      return String.format("%d:%02d:%02d", hours, minutes, seconds);
    }
    return String.format("%02d:%02d", minutes, seconds);
  }
}
