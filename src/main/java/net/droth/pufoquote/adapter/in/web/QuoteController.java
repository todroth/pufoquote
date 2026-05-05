package net.droth.pufoquote.adapter.in.web;

import lombok.RequiredArgsConstructor;
import net.droth.pufoquote.adapter.in.web.dto.QuoteViewModel;
import net.droth.pufoquote.domain.model.Category;
import net.droth.pufoquote.domain.model.Quote;
import net.droth.pufoquote.domain.port.in.GetRandomQuoteUseCase;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Thymeleaf controller for the main quote page. */
@Controller
@RequiredArgsConstructor
public class QuoteController {

  private final GetRandomQuoteUseCase getRandomQuoteUseCase;

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

  private QuoteViewModel toViewModel(Quote quote) {
    return new QuoteViewModel(
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
