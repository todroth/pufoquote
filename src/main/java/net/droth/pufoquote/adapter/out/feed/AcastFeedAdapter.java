package net.droth.pufoquote.adapter.out.feed;

import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.droth.pufoquote.domain.model.Episode;
import net.droth.pufoquote.domain.port.out.FeedPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Feed adapter that parses podcast episodes from an Acast RSS feed. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AcastFeedAdapter implements FeedPort {

  @Value("${podcast.feed-url}")
  private final String feedUrl;

  @Override
  public List<Episode> fetchEpisodes() {
    try {
      SyndFeedInput input = new SyndFeedInput();
      SyndFeed feed;
      try (XmlReader reader = new XmlReader(URI.create(feedUrl).toURL().openStream())) {
        feed = input.build(reader);
      }

      List<Episode> episodes = new ArrayList<>();
      for (SyndEntry entry : feed.getEntries()) {
        String mp3Url =
            entry.getEnclosures().stream()
                .filter(e -> e.getType() != null && e.getType().contains("audio"))
                .map(SyndEnclosure::getUrl)
                .findFirst()
                .orElse(null);

        if (mp3Url == null) {
          log.debug("Skipping entry without audio enclosure: {}", entry.getTitle());
          continue;
        }

        // Older episodes use WordPress URLs as GUIDs; newer ones use UUIDs — both are valid IDs
        String id = entry.getUri() != null ? entry.getUri() : entry.getLink();
        LocalDate date = toLocalDate(entry.getPublishedDate());

        episodes.add(new Episode(id, entry.getTitle(), date, entry.getLink(), mp3Url));
      }

      log.info("Parsed {} episodes from feed", episodes.size());
      return episodes;
    } catch (Exception e) {
      throw new RuntimeException("Failed to fetch RSS feed from: " + feedUrl, e);
    }
  }

  private LocalDate toLocalDate(Date date) {
    if (date == null) {
      return null;
    }
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }
}
