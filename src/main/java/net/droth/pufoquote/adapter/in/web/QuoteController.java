package net.droth.pufoquote.adapter.in.web;

import lombok.RequiredArgsConstructor;
import net.droth.pufoquote.adapter.in.web.dto.QuoteViewModel;
import net.droth.pufoquote.domain.model.Category;
import net.droth.pufoquote.domain.model.Quote;
import net.droth.pufoquote.domain.model.QuoteContext;
import net.droth.pufoquote.domain.port.in.GetQuoteByIdUseCase;
import net.droth.pufoquote.domain.port.in.GetQuoteContextUseCase;
import net.droth.pufoquote.domain.port.in.GetRandomQuoteUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/** Thymeleaf controller for the main quote page. */
@Controller
@RequiredArgsConstructor
public class QuoteController {

  private final GetRandomQuoteUseCase getRandomQuoteUseCase;
  private final GetQuoteByIdUseCase getQuoteByIdUseCase;
  private final GetQuoteContextUseCase getQuoteContextUseCase;

  @GetMapping("/")
  public String index(
      @RequestParam(name = "category", defaultValue = "RANDOM") String categoryParam, Model model) {
    Category category = Category.fromString(categoryParam);
    getRandomQuoteUseCase
        .getRandomQuote(category)
        .map(this::toViewModel)
        .ifPresent(vm -> model.addAttribute("quote", vm));
    model.addAttribute("categories", Category.uiValues());
    model.addAttribute("currentCategory", category);
    return "index";
  }

  @GetMapping("/quote/{id}")
  public String quoteById(@PathVariable String id, Model model) {
    getQuoteByIdUseCase
        .getById(id)
        .map(this::toViewModel)
        .ifPresent(vm -> model.addAttribute("quote", vm));
    model.addAttribute("categories", Category.uiValues());
    model.addAttribute("currentCategory", Category.RANDOM);
    return "index";
  }

  @GetMapping(value = "/api/quote/{id}")
  @ResponseBody
  public ResponseEntity<QuoteViewModel> apiQuoteById(@PathVariable String id) {
    return getQuoteByIdUseCase
        .getById(id)
        .map(this::toViewModel)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping(value = "/api/quote")
  @ResponseBody
  public ResponseEntity<QuoteViewModel> apiQuote(
      @RequestParam(name = "category", defaultValue = "RANDOM") String categoryParam) {
    Category category = Category.fromString(categoryParam);
    return getRandomQuoteUseCase
        .getRandomQuote(category)
        .map(this::toViewModel)
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

  private QuoteViewModel toViewModel(Quote quote) {
    return new QuoteViewModel(
        quote.id(),
        quote.text(),
        quote.episodeName(),
        quote.episodeDate(),
        formatTimestamp(quote.startSeconds()),
        quote.episodeUrl());
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
