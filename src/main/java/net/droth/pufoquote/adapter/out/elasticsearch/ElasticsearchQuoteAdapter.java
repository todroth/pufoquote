package net.droth.pufoquote.adapter.out.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.FieldValueFactorModifier;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.droth.pufoquote.domain.model.Category;
import net.droth.pufoquote.domain.model.Quote;
import net.droth.pufoquote.domain.port.out.QuoteRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Reads and writes quotes to Elasticsearch using a delete-then-insert approach. */
@Slf4j
@Component
@RequiredArgsConstructor
class ElasticsearchQuoteAdapter implements QuoteRepositoryPort {

  private static final String INDEX = "quotes";

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
    } catch (IOException e) {
      log.warn("Could not delete all quotes: {}", e.getMessage());
    }
  }

  @Override
  public Optional<Quote> findRandom(Category category) {
    try {
      Query filterQuery = buildQuery(category);
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
                                          // random_score provides variety
                                          .functions(f -> f.randomScore(r -> r))
                                          // qualityScore weights higher-scoring quotes more often
                                          .functions(
                                              f ->
                                                  f.fieldValueFactor(
                                                      fvf ->
                                                          fvf.field("qualityScore")
                                                              .factor(1.0)
                                                              .modifier(
                                                                  FieldValueFactorModifier.None)
                                                              .missing(3.0)))
                                          .scoreMode(
                                              co.elastic.clients.elasticsearch._types.query_dsl
                                                  .FunctionScoreMode.Multiply))),
              QuoteDocument.class);

      return response.hits().hits().stream().findFirst().map(hit -> toDomain(hit.source()));
    } catch (IOException e) {
      log.error("Failed to find random quote: {}", e.getMessage(), e);
      return Optional.empty();
    }
  }

  private Query buildQuery(Category category) {
    Query scoreFilter =
        Query.of(
            q ->
                q.range(r -> r.number(n -> n.field("qualityScore").gte((double) minQualityScore))));
    if (category == null || category == Category.RANDOM) {
      return scoreFilter;
    }
    Query categoryFilter =
        Query.of(q -> q.term(t -> t.field("categories").value(category.name().toLowerCase())));
    return Query.of(q -> q.bool(b -> b.filter(scoreFilter).filter(categoryFilter)));
  }

  private void deleteExisting(String episodeId) {
    try {
      esClient.deleteByQuery(
          d -> d.index(INDEX).query(q -> q.term(t -> t.field("episodeId").value(episodeId))));
    } catch (IOException e) {
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
