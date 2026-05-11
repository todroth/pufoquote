package net.droth.pufoquote.adapter.in.web.dto;

/** View model for an episode summary shown on the episodes list page. */
public record EpisodeSummaryViewModel(
    String episodeId,
    String encodedId,
    String episodeName,
    String episodeDate,
    String episodeUrl) {}
