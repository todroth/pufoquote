package net.droth.pufoquote.domain.model;

/** Episode metadata used for the episodes list page. */
public record EpisodeSummary(
    String episodeId, String episodeName, String episodeDate, String episodeUrl) {}
