package net.droth.pufoquote.adapter.out.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.droth.pufoquote.domain.model.Category;
import net.droth.pufoquote.domain.model.EpisodeSummary;
import net.droth.pufoquote.domain.model.Quote;
import net.droth.pufoquote.domain.model.QuoteContext;
import net.droth.pufoquote.domain.port.out.QuoteRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Reads and writes quotes to Elasticsearch using a delete-then-insert approach. */
@Slf4j
@Component
@RequiredArgsConstructor
class ElasticsearchQuoteAdapter implements QuoteRepositoryPort {

  private static final String INDEX = "quotes";
  // Categories with fewer than 100 score-5 quotes get a 75/25 split; others get 90/10
  private static final Set<Category> LOW_STOCK_CATEGORIES =
      Set.of(Category.INTERESTING, Category.META, Category.SERIOUS);

  private enum ScoreTier {
    TOP,
    LOWER
  }

  private final QuoteEsRepository esRepository;
  private final ElasticsearchClient esClient;

  @Value("${quote.min-quality-score:4}")
  private final int minQualityScore;

  @Override
  public void saveAll(String episodeId, List<Quote> quotes) {
    deleteExisting(episodeId);
    List<QuoteDocument> docs = quotes.stream().map(this::toDocument).toList();
    esRepository.saveAll(docs);
  }

  @Override
  public boolean existsByEpisodeId(String episodeId) {
    return esRepository.existsByEpisodeId(episodeId);
  }

  @Override
  public void deleteAll() {
    try {
      esClient.deleteByQuery(d -> d.index(INDEX).query(q -> q.matchAll(m -> m)));
    } catch (IOException | ElasticsearchException e) {
      log.warn("Could not delete all quotes: {}", e.getMessage());
    }
  }

  @Override
  public Optional<Quote> findRandom(Category category) {
    try {
      Query filterQuery = buildQuery(category, pickTier(category));
      var response =
          esClient.search(
              s ->
                  s.index(INDEX)
                      .size(1)
                      .query(
                          q ->
                              q.functionScore(
                                  fs ->
                                      fs.query(filterQuery)
                                          .functions(f -> f.randomScore(r -> r))
                                          .scoreMode(FunctionScoreMode.Multiply))),
              QuoteDocument.class);
      return response.hits().hits().stream().findFirst().map(hit -> toDomain(hit.source()));
    } catch (IOException | ElasticsearchException e) {
      log.error("Failed to find random quote: {}", e.getMessage(), e);
      return Optional.empty();
    }
  }

  @Override
  public Optional<Quote> findById(String id) {
    try {
      var response = esClient.get(g -> g.index(INDEX).id(id), QuoteDocument.class);
      return Optional.ofNullable(response.source()).map(this::toDomain);
    } catch (IOException | ElasticsearchException e) {
      log.error("Failed to find quote by id {}: {}", id, e.getMessage(), e);
      return Optional.empty();
    }
  }

  @Override
  public QuoteContext findContext(String episodeId, double startSeconds, int count) {
    try {
      Query episodeFilter = Query.of(q -> q.term(t -> t.field("episodeId").value(episodeId)));

      var beforeResp =
          esClient.search(
              s ->
                  s.index(INDEX)
                      .size(count)
                      .sort(so -> so.field(f -> f.field("startSeconds").order(SortOrder.Desc)))
                      .query(
                          q ->
                              q.bool(
                                  b ->
                                      b.filter(episodeFilter)
                                          .filter(
                                              f ->
                                                  f.range(
                                                      r ->
                                                          r.number(
                                                              n ->
                                                                  n.field("startSeconds")
                                                                      .lt(startSeconds)))))),
              QuoteDocument.class);

      var afterResp =
          esClient.search(
              s ->
                  s.index(INDEX)
                      .size(count)
                      .sort(so -> so.field(f -> f.field("startSeconds").order(SortOrder.Asc)))
                      .query(
                          q ->
                              q.bool(
                                  b ->
                                      b.filter(episodeFilter)
                                          .filter(
                                              f ->
                                                  f.range(
                                                      r ->
                                                          r.number(
                                                              n ->
                                                                  n.field("startSeconds")
                                                                      .gt(startSeconds)))))),
              QuoteDocument.class);

      // before is returned DESC; reverse to chronological order
      List<String> before = new ArrayList<>();
      beforeResp.hits().hits().forEach(h -> before.add(0, h.source().getText()));

      List<String> after = afterResp.hits().hits().stream().map(h -> h.source().getText()).toList();

      return new QuoteContext(before, after);
    } catch (IOException | ElasticsearchException e) {
      log.error("Failed to find context for episodeId {}: {}", episodeId, e.getMessage(), e);
      return new QuoteContext(List.of(), List.of());
    }
  }

  private ScoreTier pickTier(Category category) {
    double threshold = LOW_STOCK_CATEGORIES.contains(category) ? 0.75 : 0.90;
    return ThreadLocalRandom.current().nextDouble() < threshold ? ScoreTier.TOP : ScoreTier.LOWER;
  }

  private Query buildQuery(Category category, ScoreTier tier) {
    Query scoreFilter =
        tier == ScoreTier.TOP
            ? Query.of(q -> q.term(t -> t.field("qualityScore").value(5L)))
            : Query.of(
                q ->
                    q.range(
                        r ->
                            r.number(
                                n ->
                                    n.field("qualityScore")
                                        .gte((double) minQualityScore)
                                        .lt(5.0))));
    if (category == null || category == Category.RANDOM) {
      return Query.of(q -> q.bool(b -> b.must(m -> m.matchAll(ma -> ma)).filter(scoreFilter)));
    }
    Query categoryFilter =
        Query.of(q -> q.term(t -> t.field("categories").value(category.name().toLowerCase())));
    return Query.of(
        q ->
            q.bool(
                b -> b.must(m -> m.matchAll(ma -> ma)).filter(scoreFilter).filter(categoryFilter)));
  }

  @Override
  public List<EpisodeSummary> findAllEpisodes() {
    try {
      var response =
          esClient.search(
              s ->
                  s.index(INDEX)
                      .size(500)
                      .source(
                          src ->
                              src.filter(
                                  f ->
                                      f.includes(
                                          List.of(
                                              "episodeId",
                                              "episodeName",
                                              "episodeDate",
                                              "episodeUrl"))))
                      .collapse(c -> c.field("episodeId"))
                      .sort(so -> so.field(f -> f.field("episodeDate").order(SortOrder.Desc))),
              QuoteDocument.class);
      return response.hits().hits().stream()
          .map(hit -> hit.source())
          .filter(Objects::nonNull)
          .map(
              doc ->
                  new EpisodeSummary(
                      doc.getEpisodeId(),
                      doc.getEpisodeName(),
                      doc.getEpisodeDate(),
                      doc.getEpisodeUrl()))
          .toList();
    } catch (IOException | ElasticsearchException e) {
      log.error("Failed to find all episodes: {}", e.getMessage(), e);
      return List.of();
    }
  }

  @Override
  public List<Quote> findAllByEpisodeId(String episodeId) {
    try {
      var response =
          esClient.search(
              s ->
                  s.index(INDEX)
                      .size(1000)
                      .query(q -> q.term(t -> t.field("episodeId").value(episodeId))),
              QuoteDocument.class);
      return response.hits().hits().stream()
          .map(hit -> toDomain(hit.source()))
          .filter(Objects::nonNull)
          .toList();
    } catch (IOException | ElasticsearchException e) {
      log.error("Failed to find quotes for episode {}: {}", episodeId, e.getMessage(), e);
      return List.of();
    }
  }

  private void deleteExisting(String episodeId) {
    try {
      esClient.deleteByQuery(
          d -> d.index(INDEX).query(q -> q.term(t -> t.field("episodeId").value(episodeId))));
    } catch (IOException | ElasticsearchException e) {
      log.warn("Could not delete existing quotes for episode {}: {}", episodeId, e.getMessage());
    }
  }

  private QuoteDocument toDocument(Quote quote) {
    QuoteDocument doc = new QuoteDocument();
    doc.setId(quote.id());
    doc.setEpisodeId(quote.episodeId());
    doc.setEpisodeName(quote.episodeName());
    doc.setEpisodeDate(quote.episodeDate());
    doc.setEpisodeUrl(quote.episodeUrl());
    doc.setMp3Url(quote.mp3Url());
    doc.setStartSeconds(quote.startSeconds());
    doc.setText(quote.text());
    doc.setWordCount(quote.wordCount());
    doc.setQualityScore(quote.qualityScore());
    doc.setCategories(quote.categories().stream().map(c -> c.name().toLowerCase()).toList());
    return doc;
  }

  private Quote toDomain(QuoteDocument doc) {
    if (doc == null) {
      return null;
    }
    List<Category> categories =
        doc.getCategories() == null
            ? List.of()
            : doc.getCategories().stream()
                .map(s -> Category.fromString(s.toUpperCase()))
                .filter(c -> c != Category.NONE && c != Category.RANDOM)
                .toList();
    return new Quote(
        doc.getId(),
        doc.getEpisodeId(),
        doc.getEpisodeName(),
        doc.getEpisodeDate(),
        doc.getEpisodeUrl(),
        doc.getMp3Url(),
        doc.getStartSeconds(),
        doc.getText(),
        doc.getWordCount(),
        doc.getQualityScore(),
        categories);
  }
}
