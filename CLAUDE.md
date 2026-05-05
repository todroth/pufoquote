# pufoquote — Claude Code Guide

Random quote generator for *DAS PODCAST UFO*. Spring Boot + Elasticsearch backend, Thymeleaf frontend. Transcribes new podcast episodes automatically and serves categorized quotes.

---

## Build & Run

### Prerequisites

- Java 25
- Maven (use IntelliJ's bundled Maven if `mvn` is not on PATH)
- Docker (for Elasticsearch)
- `ffmpeg` (for MP3 compression during indexing)

### Maven binary

```bash
"/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn"
```

Use this path when `mvn` is not available on `$PATH`.

### Local development

```bash
# 1. Start Elasticsearch
docker compose up elasticsearch -d

# 2. Fill in .env (GROQ_API_KEY and ADMIN_PASSWORD are required)
# .env is gitignored; edit it directly

# 3. Run the app (sources env vars from .env)
set -a && source .env && set +a && \
  "/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" spring-boot:run
```

App starts at http://localhost:8080

### Build JAR

```bash
"/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" clean package -DskipTests
# Output: target/pufoquote-1.0-SNAPSHOT.jar
```

### Apply resource changes during spring-boot:run

Template and other resource changes are NOT live automatically. Copy them to `target/` with:

```bash
"/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" resources:resources
```

Then refresh the browser — no restart needed.

### Code quality

```bash
# Auto-format with google-java-format
"/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" spotless:apply

# Check formatting + Checkstyle (runs on mvn verify)
"/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" verify
```

All Java code must be formatted with **Google Java Format** (2-space indent). Run `spotless:apply` before committing.

### Commit messages

Plain subject line only. Do **not** add `Co-Authored-By` or any other trailers.

---

## Docker

### Full stack (app + Elasticsearch)

```bash
docker compose up --build
docker compose up --build -d   # background
```

Requires a `.env` file with `GROQ_API_KEY` and `ADMIN_PASSWORD`.

### Elasticsearch only (for local dev)

```bash
docker compose up elasticsearch -d
```

### Docker details

- **Elasticsearch**: `elasticsearch:9.3.4`, single-node, security disabled, 512m heap, data persisted in `es_data` volume
- **App image**: `eclipse-temurin:25-jre-alpine` + `ffmpeg`, exposes port 8080
- App waits for Elasticsearch healthcheck before starting (`depends_on: condition: service_healthy`)

---

## Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `GROQ_API_KEY` | Yes | — | Groq API key (transcription + categorization) |
| `ADMIN_PASSWORD` | Yes | `changeme` (Docker) | HTTP Basic auth password for `/admin/**` |
| `ELASTICSEARCH_URI` | No | `http://localhost:9200` | Elasticsearch endpoint |
| `PODCAST_FEED_URL` | No | Das Podcast UFO Acast feed | RSS feed to poll |
| `TRANSCRIPTION_CACHE_DIR` | No | `./transcriptions` | Local dir for cached transcription JSON files |

---

## Architecture

Hexagonal architecture (Ports & Adapters). The domain has zero framework dependencies.

```
┌──────────────────────────────────────────────────────────────────┐
│                         Adapters (in)                            │
│  QuoteController   AdminController   ScheduledIndexer            │
└──────────────┬───────────────────────────────────────────────────┘
               │ calls
┌──────────────▼───────────────────────────────────────────────────┐
│                       Domain Ports (in)                          │
│   GetRandomQuoteUseCase   IndexEpisodesUseCase                   │
├──────────────────────────────────────────────────────────────────┤
│                     Application Services                         │
│   GetRandomQuoteService   IndexEpisodesService                   │
├──────────────────────────────────────────────────────────────────┤
│                       Domain Ports (out)                         │
│   QuoteRepositoryPort   FeedPort                                 │
│   TranscriptionPort   TranscriptionCachePort                     │
│   CategorizationPort                                             │
└──────────────┬───────────────────────────────────────────────────┘
               │ implemented by
┌──────────────▼───────────────────────────────────────────────────┐
│                        Adapters (out)                            │
│   ElasticsearchQuoteAdapter   AcastFeedAdapter                   │
│   GroqTranscriptionAdapter    GroqCategorizationAdapter          │
│   FileSystemTranscriptionCacheAdapter                            │
└──────────────────────────────────────────────────────────────────┘
```

---

## Source Map

```
src/main/java/net/droth/pufoquote/
├── PufoquoteApplication.java
├── domain/
│   ├── model/
│   │   ├── Episode.java                record: id, title, date, episodeUrl, mp3Url
│   │   ├── Segment.java                record: startSeconds, endSeconds, text
│   │   ├── Quote.java                  record: id, episodeId, episodeName, episodeDate,
│   │   │                                       episodeUrl, mp3Url, startSeconds, text,
│   │   │                                       wordCount, categories
│   │   ├── Category.java               enum: FUNNY, ABSURD, INTERESTING, PHILOSOPHICAL,
│   │   │                                     DRAMATIC, SELF_AWARE, RANDOM, NONE
│   │   │                               — uiValues() returns display categories (excl. NONE)
│   │   │                               — fromString() parses safely, defaults to RANDOM
│   │   └── SentenceWithTimestamp.java  record: text, startSeconds
│   ├── service/
│   │   └── SentenceSplitter.java       splits Segment blocks into sentences; tracks startSeconds
│   └── port/
│       ├── in/
│       │   ├── GetRandomQuoteUseCase.java    getRandomQuote(Category)
│       │   └── IndexEpisodesUseCase.java     indexNewEpisodes()
│       └── out/
│           ├── QuoteRepositoryPort.java      saveAll, existsByEpisodeId, findRandom
│           ├── FeedPort.java                 fetchEpisodes()
│           ├── TranscriptionPort.java        transcribe(Path mp3)
│           ├── TranscriptionCachePort.java   load(), save()
│           └── CategorizationPort.java       classify(List<String> sentences)
├── application/
│   ├── GetRandomQuoteService.java      delegates to QuoteRepositoryPort
│   └── IndexEpisodesService.java       full pipeline: fetch → transcode → split → categorize → index
│                                       AtomicBoolean guard prevents concurrent runs
└── adapter/
    ├── in/
    │   ├── web/
    │   │   ├── QuoteController.java    GET / — renders index.html with random quote
    │   │   ├── AdminController.java    POST /admin/index-all (async, HTTP Basic)
    │   │   ├── SecurityConfig.java     HTTP Basic on /admin/**, CSRF disabled there
    │   │   └── dto/
    │   │       └── QuoteViewModel.java record: text, episodeName, episodeDate, timestamp, episodeUrl
    │   └── scheduler/
    │       └── ScheduledIndexer.java   cron: 0 0 6 * * * (daily 6am UTC)
    └── out/
        ├── elasticsearch/
        │   ├── ElasticsearchQuoteAdapter.java  QuoteRepositoryPort impl; delete-before-save + random_score query
        │   ├── QuoteDocument.java              @Document(index="quotes")
        │   └── QuoteEsRepository.java          Spring Data ES interface
        ├── feed/
        │   └── AcastFeedAdapter.java           Rome RSS parser; episode ID from <guid>
        ├── groq/
        │   ├── GroqConfig.java                 @Configuration: WebClient bean with Bearer auth
        │   ├── GroqTranscriptionAdapter.java   WebClient → Groq Whisper API (verbose_json)
        │   └── GroqCategorizationAdapter.java  WebClient → Groq chat completion (batch 30 sentences)
        └── filesystem/
            ├── FileSystemTranscriptionCacheAdapter.java  JSON cache; atomic write via .tmp move
            └── JacksonConfig.java                        @Configuration: ObjectMapper bean
```

---

## Indexing Pipeline

`IndexEpisodesService.indexNewEpisodes()`:

1. `AtomicBoolean` guard — at most one run at a time
2. Fetch all episodes from RSS feed (ordered newest-first)
3. Skip episodes already in ES (`existsByEpisodeId`)
4. Check filesystem cache for existing transcription JSON
5. If not cached: download MP3 → compress with ffmpeg (mono 16kHz 32kbps) → call Groq Whisper → cache result
6. `SentenceSplitter.split(segments)` → sentences with timestamps (min 6 words)
7. Batch 30 sentences → `GroqCategorizationAdapter.classify()` — returns one category per sentence
8. Drop sentences with category `NONE`
9. `ElasticsearchQuoteAdapter.saveAll()` — delete existing + bulk insert (idempotent)
10. Delete temp files

ffmpeg compression keeps files under Groq's 25 MB upload limit.

---

## Sentence Splitting

`SentenceSplitter` merges audio blocks (which often cut mid-sentence) into complete sentences:

- Appends blocks sequentially; detects `.` `?` `!` followed by whitespace + uppercase (or EOF)
- Each sentence keeps the `startSeconds` of the block where it started
- Sentences with fewer than 6 words are dropped
- No NLP library — pure Java regex

---

## Categorization

`GroqCategorizationAdapter` calls `https://api.groq.com/openai/v1/chat/completions`:

- Model: `llama-3.1-8b-instant` (configurable via `groq.categorization-model`)
- Batches of 30 sentences per request
- Prompt: asks for exactly one label per sentence from: `funny, absurd, interesting, philosophical, dramatic, self_aware, none`
- Response: JSON array of label strings, parsed and mapped to `Category` enum
- On parse failure: defaults all sentences to `NONE`
- Strips markdown code fences if the model wraps its JSON output

---

## Elasticsearch Index

**Index**: `quotes`
**Settings**: `src/main/resources/elasticsearch/settings.json` — German analyzer

| Field | Type | Notes |
|---|---|---|
| `id` | Keyword | UUID |
| `episodeId` | Keyword | From RSS `<guid>` |
| `episodeName` | Text | Episode title |
| `episodeDate` | Keyword | ISO date string |
| `episodeUrl` | Keyword | Link to episode page |
| `mp3Url` | Keyword | Direct audio file URL |
| `startSeconds` | Double | Sentence start time |
| `text` | Text | Sentence text, German analyzer |
| `wordCount` | Integer | Word count |
| `categories` | Keyword[] | e.g. `["funny"]` (lowercase enum name) |

Random quote query: `function_score` with `random_score` + optional `term` filter on `categories`.

---

## Groq APIs Used

| API | Endpoint | Model | Purpose |
|---|---|---|---|
| Whisper transcription | `/openai/v1/audio/transcriptions` | `whisper-large-v3-turbo` | MP3 → segments |
| Chat completion | `/openai/v1/chat/completions` | `llama-3.1-8b-instant` | sentence → category |

---

## Episode IDs

Episode IDs come from the RSS `<guid>` element. Older episodes use WordPress URLs (e.g. `http://podcast-ufo.fail/?p=1041`) as GUIDs; newer ones use UUIDs. The filesystem cache sanitizes these to safe filenames (`http---podcast-ufo-fail--p-1041.json`).

---

## Frontend

Single Thymeleaf page (`templates/index.html`):

- Header always reads **DAS PODCAST UFO** (all-caps, no exceptions)
- Category buttons: `/?category=FUNNY` — active category gets `.active` class
- Quote card: quote text, episode name (linked to episode URL), timestamp (`HH:MM:SS` or `MM:SS`)
- "Nächstes Zitat" button: reloads page with same `category` param — no JavaScript required
- Styling: dark background (`#0d0d0d`), Bootstrap 5 from CDN

---

## Admin Endpoints

```
POST /admin/index-all   — index all episodes not yet in ES (async)
```

HTTP Basic auth: username `admin`, password from `ADMIN_PASSWORD` env var.

---

## Common Tasks

```bash
# Trigger full index (skips already-indexed episodes)
curl -u admin:$ADMIN_PASSWORD -X POST http://localhost:8080/admin/index-all

# Check indexed quote count
curl http://localhost:9200/quotes/_count

# Check quotes per category
curl -s http://localhost:9200/quotes/_search \
  -H "Content-Type: application/json" \
  -d '{"size":0,"aggs":{"by_cat":{"terms":{"field":"categories","size":20}}}}' \
  | python3 -m json.tool

# Check Elasticsearch cluster health
curl http://localhost:9200/_cluster/health
```

---

## Sister Project

**pufosearch** (`/Users/tobi/projects/pufosearch`) is the search engine for the same transcripts. It shares the same transcription JSON cache directory and the same RSS feed. It uses a separate Elasticsearch index (`podcast_segments`).
