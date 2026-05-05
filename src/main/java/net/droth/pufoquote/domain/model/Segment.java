package net.droth.pufoquote.domain.model;

/** A transcribed audio segment with start/end timestamps and its text content. */
public record Segment(double startSeconds, double endSeconds, String text) {}
