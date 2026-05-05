package net.droth.pufoquote.domain.model;

import java.util.List;

/** A categorized quote sentence extracted from a podcast episode transcription. */
public record Quote(
    String id,
    String episodeId,
    String episodeName,
    String episodeDate,
    String episodeUrl,
    String mp3Url,
    double startSeconds,
    String text,
    int wordCount,
    int qualityScore,
    List<Category> categories) {}
