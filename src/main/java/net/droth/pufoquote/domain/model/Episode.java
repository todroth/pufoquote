package net.droth.pufoquote.domain.model;

import java.time.LocalDate;

/** Domain model representing a podcast episode. */
public record Episode(String id, String title, LocalDate date, String episodeUrl, String mp3Url) {}
