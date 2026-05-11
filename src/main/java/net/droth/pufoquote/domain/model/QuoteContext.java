package net.droth.pufoquote.domain.model;

import java.util.List;

/** The sentences immediately before and after a quote in the same episode. */
public record QuoteContext(List<String> before, List<String> after) {}
