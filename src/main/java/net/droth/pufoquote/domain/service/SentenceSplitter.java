package net.droth.pufoquote.domain.service;

import java.util.ArrayList;
import java.util.List;
import net.droth.pufoquote.domain.model.Segment;
import net.droth.pufoquote.domain.model.SentenceWithTimestamp;
import org.springframework.stereotype.Component;

/**
 * Splits a list of audio blocks into complete sentences, preserving the start timestamp of the
 * block where each sentence began.
 *
 * <p>Blocks from the transcription API often cut mid-sentence, so this class concatenates them and
 * detects sentence boundaries (., ?, ! followed by whitespace + uppercase, or end of text).
 */
@Component
public class SentenceSplitter {

  private static final int MIN_WORD_COUNT = 6;

  /** Splits the given audio blocks into complete sentences with timestamps. */
  public List<SentenceWithTimestamp> split(List<Segment> blocks) {
    List<SentenceWithTimestamp> result = new ArrayList<>();
    StringBuilder buffer = new StringBuilder();
    double sentenceStartSeconds = 0.0;

    for (Segment block : blocks) {
      if (buffer.isEmpty()) {
        sentenceStartSeconds = block.startSeconds();
      } else {
        buffer.append(' ');
      }
      buffer.append(block.text().trim());

      int start = 0;
      int i = 0;
      while (i < buffer.length()) {
        char c = buffer.charAt(i);
        if (c == '.' || c == '?' || c == '!') {
          // Find next non-space character
          int j = i + 1;
          while (j < buffer.length() && buffer.charAt(j) == ' ') {
            j++;
          }
          // Sentence boundary: next char is uppercase or we reached end of buffer
          if (j >= buffer.length() || Character.isUpperCase(buffer.charAt(j))) {
            String sentence = buffer.substring(start, i + 1).trim();
            if (wordCount(sentence) >= MIN_WORD_COUNT) {
              result.add(new SentenceWithTimestamp(sentence, sentenceStartSeconds));
            }
            start = j;
            // Remaining text (if any) starts in this block
            sentenceStartSeconds = block.startSeconds();
            i = j;
            continue;
          }
        }
        i++;
      }

      // Keep unfinished sentence fragment for next block
      if (start > 0) {
        String remaining = start < buffer.length() ? buffer.substring(start) : "";
        buffer = new StringBuilder(remaining);
        if (buffer.isEmpty()) {
          sentenceStartSeconds = 0.0;
        }
      }
    }

    // Flush any remaining text as a final sentence
    String last = buffer.toString().trim();
    if (wordCount(last) >= MIN_WORD_COUNT) {
      result.add(new SentenceWithTimestamp(last, sentenceStartSeconds));
    }

    return result;
  }

  private static int wordCount(String text) {
    if (text == null || text.isBlank()) {
      return 0;
    }
    return text.trim().split("\\s+").length;
  }
}
