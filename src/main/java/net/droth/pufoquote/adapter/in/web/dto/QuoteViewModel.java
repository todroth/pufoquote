package net.droth.pufoquote.adapter.in.web.dto;

/** View model passed to the Thymeleaf template for rendering a single quote. */
public record QuoteViewModel(
    String text, String episodeName, String episodeDate, String timestamp, String episodeUrl) {}
