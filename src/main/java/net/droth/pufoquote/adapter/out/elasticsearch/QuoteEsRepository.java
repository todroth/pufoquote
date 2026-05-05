package net.droth.pufoquote.adapter.out.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/** Spring Data Elasticsearch repository for {@link QuoteDocument}. */
public interface QuoteEsRepository extends ElasticsearchRepository<QuoteDocument, String> {
  boolean existsByEpisodeId(String episodeId);
}
