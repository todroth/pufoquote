package net.droth.pufoquote.adapter.out.elasticsearch;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

/** Elasticsearch document representing a categorized podcast quote. */
@Document(indexName = "quotes")
@Setting(settingPath = "elasticsearch/settings.json")
@Getter
@Setter
@NoArgsConstructor
public class QuoteDocument {

  @Id private String id;

  @Field(type = FieldType.Keyword)
  private String episodeId;

  @Field(type = FieldType.Text)
  private String episodeName;

  @Field(type = FieldType.Keyword)
  private String episodeDate;

  @Field(type = FieldType.Keyword)
  private String episodeUrl;

  @Field(type = FieldType.Keyword)
  private String mp3Url;

  @Field(type = FieldType.Double)
  private double startSeconds;

  @Field(type = FieldType.Text, analyzer = "german")
  private String text;

  @Field(type = FieldType.Integer)
  private int wordCount;

  @Field(type = FieldType.Integer)
  private int qualityScore;

  @Field(type = FieldType.Keyword)
  private List<String> categories;
}
