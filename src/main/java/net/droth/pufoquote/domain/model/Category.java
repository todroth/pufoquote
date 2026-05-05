package net.droth.pufoquote.domain.model;

import java.util.List;

/** Quote categories shown in the UI. NONE means the sentence is skipped during indexing. */
public enum Category {
  FUNNY("Lustig"),
  DRAMATIC("Dramatisch"),
  INTERESTING("Interessant"),
  SERIOUS("Ernst"),
  META("Meta"),
  RANDOM("Alle"),
  NONE(null);

  private final String displayName;

  Category(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  /** Categories shown as filter buttons in the UI (excludes NONE). */
  public static List<Category> uiValues() {
    return List.of(RANDOM, FUNNY, DRAMATIC, INTERESTING, SERIOUS, META);
  }

  /** Parse a category from a string, defaulting to RANDOM on unknown input. */
  public static Category fromString(String s) {
    if (s == null || s.isBlank()) {
      return RANDOM;
    }
    try {
      return valueOf(s.toUpperCase());
    } catch (IllegalArgumentException e) {
      return RANDOM;
    }
  }
}
