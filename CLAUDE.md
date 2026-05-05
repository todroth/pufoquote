# pufoquote — Claude Code Guide

Random quote generator for *DAS PODCAST UFO*. Spring Boot + Elasticsearch + Redis backend, Thymeleaf + vanilla JS frontend. Transcribes new podcast episodes automatically, scores sentences with an LLM, and serves categorized quotes. Visitors can vote for quotes; the best-of page ranks them by vote count.

---

## Build & Run

### Prerequisites

- Java 25
- Maven (use IntelliJ's bundled Maven if `mvn` is not on PATH)
- Docker (for Elasticsearch and Redis)
- `ffmpeg` (for MP3 compression during indexing)

### Maven binary

```bash
"/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn"
```

Use this path when `mvn` is not available on `$PATH`.

### Local development

```bash
# 1. Start Elasticsearch and Redis
docker compose up elasticsearch redis -d

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

Template, CSS, and JS changes are NOT live automatically. Copy them to `target/` with:

```bash
"/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" resources:resources
```

Then refresh the browser — no restart needed. Java changes require a full restart.

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

### Full stack (app + Elasticsearch + Redis)

```bash
docker compose up --build
docker compose up --build -d   # background
```

Requires a `.env` file with `GROQ_API_KEY` and `ADMIN_PASSWORD`.

### Elasticsearch + Redis only (for local dev)

```bash
docker compose up elasticsearch redis -d
```

### Docker details

- **Elasticsearch**: `elasticsearch:9.3.4`, single-node, security disabled, 512m heap, data persisted in `es_data` volume
- **Redis**: `redis:8-alpine`, data persisted in `redis_data` volume, port 6379 exposed to host (for local dev)
- **App image**: `eclipse-temurin:25-jre-alpine` + `ffmpeg`, exposes port 8080
- App waits for Elasticsearch and Redis healthchecks before starting (`depends_on: condition: service_healthy`)

---

## Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `GROQ_API_KEY` | Yes | — | Groq API key (transcription + categorization) |
| `ADMIN_PASSWORD` | Yes | `changeme` (Docker) | HTTP Basic auth password for `/admin/**` |
| `ELASTICSEARCH_URI` | No | `http://localhost:9200` | Elasticsearch endpoint |
| `REDIS_URI` | No | `redis://localhost:6379` | Redis endpoint |
| `PODCAST_FEED_URL` | No | Das Podcast UFO Acast feed | RSS feed to poll |
| `TRANSCRIPTION_CACHE_DIR` | No | `./transcriptions` | Local dir for cached transcription JSON files |
| `quote.min-quality-score` | No | `4` | Minimum score (1–5) for quotes served to the UI |

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
│   GetRandomQuoteUseCase   GetQuoteByIdUseCase                    │
│   GetQuoteContextUseCase  IndexEpisodesUseCase                   │
│   VoteForQuoteUseCase     GetVoteCountUseCase                    │
│   GetBestOfQuotesUseCase                                         │
├──────────────────────────────────────────────────────────────────┤
│                     Application Services                         │
│   GetRandomQuoteService   GetQuoteByIdService                    │
│   GetQuoteContextService  IndexEpisodesService                   │
│   VoteForQuoteService     GetVoteCountService                    │
│   GetBestOfService                                               │
├──────────────────────────────────────────────────────────────────┤
│                       Domain Ports (out)                         │
│   QuoteRepositoryPort   FeedPort                                 │
│   TranscriptionPort   TranscriptionCachePort                     │
│   CategorizationPort   VoteRepositoryPort                        │
└──────────────┬───────────────────────────────────────────────────┘
               │ implemented by
┌──────────────▼───────────────────────────────────────────────────┐
│                        Adapters (out)                            │
│   ElasticsearchQuoteAdapter   AcastFeedAdapter                   │
│   GroqTranscriptionAdapter    GroqCategorizationAdapter          │
│   FileSystemTranscriptionCacheAdapter                            │
│   RedisVoteAdapter                                               │
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
│   │   │                                       wordCount, qualityScore, categories
│   │   ├── Classification.java         record: category, qualityScore — returned by CategorizationPort
│   │   ├── Category.java               enum: FUNNY, DRAMATIC, INTERESTING, SERIOUS, META, RANDOM, NONE
│   │   │                               — uiValues() returns display categories (excl. NONE)
│   │   │                               — fromString() parses safely, defaults to RANDOM
│   │   ├── QuoteContext.java           record: before List<String>, after List<String>
│   │   ├── VoteResult.java             record: accepted boolean, voteCount long
│   │   ├── BestOfQuote.java            record: quote Quote, voteCount long
│   │   └── SentenceWithTimestamp.java  record: text, startSeconds
│   ├── service/
│   │   └── SentenceSplitter.java       splits Segment blocks into sentences; tracks startSeconds
│   └── port/
│       ├── in/
│       │   ├── GetRandomQuoteUseCase.java    getRandomQuote(Category)
│       │   ├── GetQuoteByIdUseCase.java      getById(String id) → Optional<Quote>
│       │   ├── GetQuoteContextUseCase.java   getContext(String quoteId) → Optional<QuoteContext>
│       │   ├── IndexEpisodesUseCase.java     indexNewEpisodes(), reindexAll()
│       │   ├── VoteForQuoteUseCase.java      vote(quoteId, Set<String> alreadyVotedIds) → VoteResult
│       │   ├── GetVoteCountUseCase.java      getVoteCount(quoteId) → long
│       │   └── GetBestOfQuotesUseCase.java   getTopQuotes(int limit) → List<BestOfQuote>
│       └── out/
│           ├── QuoteRepositoryPort.java      saveAll, existsByEpisodeId, deleteAll, findRandom,
│           │                                 findById, findContext(episodeId, startSeconds, count)
│           ├── FeedPort.java                 fetchEpisodes()
│           ├── TranscriptionPort.java        transcribe(Path mp3)
│           ├── TranscriptionCachePort.java   load(), save()
│           ├── CategorizationPort.java       classify(List<String>) → List<Classification>
│           └── VoteRepositoryPort.java       incrementVote, getVoteCount, getTopVotedQuoteIds(int)
├── application/
│   ├── GetRandomQuoteService.java      delegates to QuoteRepositoryPort.findRandom
│   ├── GetQuoteByIdService.java        delegates to QuoteRepositoryPort.findById
│   ├── GetQuoteContextService.java     looks up quote by id, then calls findContext(episodeId, startSeconds, 2)
│   ├── IndexEpisodesService.java       full pipeline: fetch → transcode → split → categorize → index
│   │                                   reindexAll() drops ES index then re-processes all cached episodes
│   │                                   AtomicBoolean guard prevents concurrent runs
│   ├── VoteForQuoteService.java        checks alreadyVotedIds set → incrementVote → return VoteResult
│   ├── GetVoteCountService.java        delegates to VoteRepositoryPort.getVoteCount
│   └── GetBestOfService.java           top IDs from Redis → fetch each from QuoteRepositoryPort
└── adapter/
    ├── in/
    │   ├── web/
    │   │   ├── QuoteController.java    GET /, GET /quote/{id}, GET /best — Thymeleaf pages
    │   │   │                           GET /api/quote, GET /api/quote/{id} — JSON endpoints
    │   │   │                           GET /api/quote/{id}/context — QuoteContext JSON
    │   │   │                           POST /api/quote/{id}/vote — vote endpoint (cookie dedup)
    │   │   ├── AdminController.java    POST /admin/index-all, POST /admin/reindex-all (HTTP Basic)
    │   │   ├── SecurityConfig.java     HTTP Basic on /admin/**; CSRF disabled for /admin/** and
    │   │   │                           /api/quote/*/vote (safe: cookie is HttpOnly+SameSite=Strict)
    │   │   └── dto/
    │   │       ├── QuoteViewModel.java record: id, text, episodeName, episodeDate, timestamp,
    │   │       │                               episodeUrl, voteCount, alreadyVoted
    │   │       │                               NOTE: boolean record accessors must be called with ()
    │   │       │                               in Thymeleaf (e.g. ${quote.alreadyVoted()}) — SpEL
    │   │       │                               does not resolve boolean record components as properties
    │   │       ├── VoteResponse.java   record: voteCount, alreadyVoted — JSON response for vote endpoint
    │   │       └── BestOfViewModel.java record: quote QuoteViewModel, voteCount long
    │   └── scheduler/
    │       └── ScheduledIndexer.java   cron: 0 0 6 * * * (daily 6am UTC)
    └── out/
        ├── elasticsearch/
        │   ├── ElasticsearchQuoteAdapter.java  QuoteRepositoryPort impl; delete-before-save;
        │   │                                   findRandom: function_score with random_score ×
        │   │                                   weight(score=5 → 100×); min-quality-score filter
        │   ├── QuoteDocument.java              @Document(index="quotes")
        │   └── QuoteEsRepository.java          Spring Data ES interface
        ├── feed/
        │   └── AcastFeedAdapter.java           Rome RSS parser; episode ID from <guid>
        ├── groq/
        │   ├── GroqConfig.java                 @Configuration: WebClient bean with Bearer auth
        │   ├── GroqTranscriptionAdapter.java   WebClient → Groq Whisper API (verbose_json)
        │   └── GroqCategorizationAdapter.java  WebClient → Groq chat completion (batch 30 sentences)
        │                                       returns "label:score" strings e.g. "funny:4"
        ├── redis/
        │   ├── RedisConfig.java                @Configuration: RedisTemplate<String,String> bean
        │   │                                   with StringRedisSerializer on all 4 serializers
        │   │                                   (avoids JDK binary keys from Boot's default template)
        │   └── RedisVoteAdapter.java           VoteRepositoryPort impl; sorted set "votes:leaderboard"
        │                                       ZINCRBY / ZSCORE / ZREVRANGE
        └── filesystem/
            ├── FileSystemTranscriptionCacheAdapter.java  JSON cache; atomic write via .tmp move
            └── JacksonConfig.java                        @Configuration: ObjectMapper bean

src/main/resources/
├── templates/
│   ├── fragments/
│   │   └── nav.html        Thymeleaf fragment for the category nav (th:fragment="categories")
│   │                       included in index.html and best.html via th:replace
│   ├── index.html          Main quote page — markup only, no inline styles or scripts
│   └── best.html           Best-of page — top 20 quotes by vote count
├── static/
│   ├── css/
│   │   └── style.css       All styles — mobile-first, Inter font, light theme
│   └── js/
│       └── main.js         AJAX quote loading (no page reloads); history.pushState for URL sync
└── elasticsearch/
    └── settings.json       German analyzer config
```

---

## Voting System

Visitors can thumbs-up a quote. Each browser gets one vote per quote, enforced server-side via a cookie.

**Cookie**: `voted_quotes` — `|`-separated quote UUIDs, HttpOnly, SameSite=Strict, 1-year expiry, capped at 100 entries (~3.7 KB) to stay under browser cookie size limits. Strictly necessary functional cookie; no consent banner required under ePrivacy Directive.

**Storage**: Redis sorted set `votes:leaderboard`. Key per quote: its UUID as a member, vote count as the score.
- Vote: `ZINCRBY votes:leaderboard 1 {quoteId}`
- Count lookup: `ZSCORE votes:leaderboard {quoteId}`
- Top N: `ZREVRANGE votes:leaderboard 0 N-1`

**Flow**: `POST /api/quote/{id}/vote` → parse `voted_quotes` cookie → `VoteForQuoteService.vote()` → if not already voted, `RedisVoteAdapter.incrementVote()` → update cookie in response → return `{voteCount, alreadyVoted}`.

**Best-of page**: `GET /best` fetches top 20 quote IDs from Redis, loads each from Elasticsearch, renders `best.html`.

**`voteCount` + `alreadyVoted`** are included in every `QuoteViewModel` (all quote API responses and Thymeleaf renders), so the like button initializes correctly on page load and after AJAX quote loads.

---

## Indexing Pipeline

`IndexEpisodesService.indexNewEpisodes()` / `reindexAll()`:

1. `AtomicBoolean` guard — at most one run at a time
2. `reindexAll()` first drops the entire `quotes` ES index via `deleteAll()`
3. Fetch all episodes from RSS feed (ordered newest-first)
4. Skip episodes already in ES when not force-reindexing
5. Check filesystem cache for existing transcription JSON
6. If not cached: download MP3 → compress with ffmpeg (mono 16kHz 32kbps) → call Groq Whisper → cache result
7. `SentenceSplitter.split(segments)` → sentences with timestamps (min 6 words)
8. Batch 30 sentences → `GroqCategorizationAdapter.classify()` → `List<Classification>` (category + score 1–5)
9. Drop sentences with category `NONE` — keep all scored non-NONE sentences regardless of score
10. `ElasticsearchQuoteAdapter.saveAll()` — delete existing episode quotes + bulk insert (idempotent)
11. Delete temp files

The quality score threshold (`quote.min-quality-score`, default 4) is applied at **read time**, not index time. This means the threshold can be changed without reindexing.

ffmpeg compression keeps files under Groq's 25 MB upload limit.

---

## Sentence Splitting

`SentenceSplitter` merges audio blocks (which often cut mid-sentence) into complete sentences:

- Appends blocks sequentially; detects `.` `?` `!` followed by whitespace + uppercase (or EOF)
- Each sentence keeps the `startSeconds` of the block where it started
- Sentences with fewer than 6 words are dropped
- No NLP library — pure Java regex

---

## Categorization & Scoring

`GroqCategorizationAdapter` calls `https://api.groq.com/openai/v1/chat/completions`:

- Model: `llama-3.1-8b-instant` (configurable via `groq.categorization-model`)
- Batches of 30 sentences per request, `max_tokens: 1200`
- Response: JSON array of `"label:score"` strings, e.g. `["funny:4","none:1",...]`
- On parse failure: defaults all sentences in the batch to `NONE/1`
- Strips markdown code fences if the model wraps its JSON output

**Categories** (5 content labels + none):

| Label | Meaning |
|---|---|
| `funny` | Makes you laugh: joke, funny observation, amusing wordplay, or amusingly bizarre statement |
| `dramatic` | Hilariously over-the-top about something trivial; maximum drama, minimum stakes |
| `interesting` | Makes you think or want to repeat it: surprising fact, unexpected insight |
| `serious` | A genuinely sincere or unexpectedly real moment in an otherwise silly podcast |
| `meta` | Hosts specifically commenting on their own podcast format, habits, or listener relationship |
| `none` | Everything else — also used for garbled/mis-transcribed German |

**Scores** (1–5):

| Score | Meaning |
|---|---|
| 1 | Garbled, mis-transcribed, or meaningless |
| 2 | Understandable but unremarkable |
| 3 | Decent standalone quote |
| 4 | Memorable — would make someone smile, think, or feel something |
| 5 | Exceptional — would make someone want to listen to the episode |

Prompt enforces: at most 2–3 non-none sentences per batch of 30; non-none must score ≥ 3.

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
| `qualityScore` | Integer | LLM score 1–5 |
| `categories` | Keyword[] | e.g. `["funny"]` (lowercase enum name) |

**Random quote query**: `function_score` with `must:matchAll` base (score 1.0) + `random_score` + weight filter (`qualityScore=5 → weight 100`). Score-5 quotes appear ~90% of the time vs score-4. Filtered by `qualityScore >= quote.min-quality-score` and optionally by category term.

---

## Frontend

**`templates/fragments/nav.html`** — category navigation fragment, included in both pages via `th:replace="~{fragments/nav :: categories}"`. Requires `categories` (list) and `currentCategory` (enum or null) in the model.

**`templates/index.html`** — main quote page. Markup and Thymeleaf expressions only, no inline styles or scripts.

**`templates/best.html`** — best-of page. Ordered list of top 20 quotes by vote count with badge, episode name/date, and link to full quote page.

**`static/css/style.css`** — all styles:
- Light theme (white background, `#111` text)
- Inter font (Google Fonts)
- Mobile-first with tablet (640px+) and desktop (1024px+) breakpoints
- 3px black top border as editorial anchor
- Responsive category pills (wrap on narrow screens)
- "Beste" pill: filled black (`.cat-btn.beste-btn`) — use the compound selector for correct specificity over `.cat-btn:hover`
- Large Georgia italic `"` quotation mark via CSS `::before`
- Fixed "Nächstes Zitat" button (full-width on mobile, centered pill on wider screens)
- Like button (`.like-btn`), dark when voted (`.liked`)
- Best-of page list (`.best-items`, `.best-rank-badge`, `.best-quote-text`, `.best-meta`)

**`static/js/main.js`** — AJAX navigation + share + context + voting:
- Intercepts clicks on category buttons that have `data-category`; buttons without it (e.g. "Beste") fall through to normal navigation
- Fetches new quote from `GET /api/quote?category=X` without page reload
- Updates DOM in place — no cursor reset, no page flicker
- `history.pushState` keeps URL in sync; `popstate` handles back/forward (both category and quote-id state)
- Falls back to full navigation if the API call fails
- **Share button**: pushes `/quote/{id}` to history and copies the URL to the clipboard (shows "copied" feedback for 1.5s)
- **Kontext button** (also triggered by clicking the quote text): toggles `GET /api/quote/{id}/context`, shows the 2 sentences before and after the quote in the same episode; resets on each new quote load
- **Like button**: `POST /api/quote/{id}/vote`; updates count in DOM and adds `.liked` class; initialised from `q.alreadyVoted` / `q.voteCount` on every quote render

---

## API Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/` | — | Thymeleaf page with random quote |
| `GET` | `/quote/{id}` | — | Thymeleaf page for a specific quote (shareable link) |
| `GET` | `/best` | — | Thymeleaf best-of page (top 20 by vote count) |
| `GET` | `/api/quote?category=X` | — | JSON quote for AJAX navigation |
| `GET` | `/api/quote/{id}` | — | JSON for a specific quote by ID |
| `GET` | `/api/quote/{id}/context` | — | `QuoteContext` — 2 sentences before/after in same episode |
| `POST` | `/api/quote/{id}/vote` | — | Cast a vote; returns `{voteCount, alreadyVoted}` |
| `POST` | `/admin/index-all` | HTTP Basic | Index new episodes (skips already-indexed) |
| `POST` | `/admin/reindex-all` | HTTP Basic | Drop all quotes and re-index everything from cache |

---

## Groq APIs Used

| API | Endpoint | Model | Purpose |
|---|---|---|---|
| Whisper transcription | `/openai/v1/audio/transcriptions` | `whisper-large-v3-turbo` | MP3 → segments |
| Chat completion | `/openai/v1/chat/completions` | `llama-3.1-8b-instant` | sentence → category + score |

---

## Episode IDs

Episode IDs come from the RSS `<guid>` element. Older episodes use WordPress URLs (e.g. `http://podcast-ufo.fail/?p=1041`) as GUIDs; newer ones use UUIDs. The filesystem cache sanitizes these to safe filenames (`http---podcast-ufo-fail--p-1041.json`).

---

## Common Tasks

```bash
# Trigger incremental index (skips already-indexed episodes)
curl -u admin:$ADMIN_PASSWORD -X POST http://localhost:8080/admin/index-all

# Drop everything and re-index all cached episodes
curl -u admin:$ADMIN_PASSWORD -X POST http://localhost:8080/admin/reindex-all

# Check total indexed quote count
curl http://localhost:9200/quotes/_count

# Check quotes per category
curl -s http://localhost:9200/quotes/_search \
  -H "Content-Type: application/json" \
  -d '{"size":0,"aggs":{"by_cat":{"terms":{"field":"categories","size":20}}}}' \
  | python3 -m json.tool

# Check score distribution
curl -s http://localhost:9200/quotes/_search \
  -H "Content-Type: application/json" \
  -d '{"size":0,"aggs":{"scores":{"terms":{"field":"qualityScore","size":10}}}}' \
  | python3 -m json.tool

# Check Elasticsearch cluster health
curl http://localhost:9200/_cluster/health

# Inspect Redis vote leaderboard
docker compose exec redis redis-cli ZREVRANGE votes:leaderboard 0 -1 WITHSCORES

# Check vote count for a specific quote
docker compose exec redis redis-cli ZSCORE votes:leaderboard <quoteId>
```

---

## Sister Project

**pufosearch** (`/Users/tobi/projects/pufosearch`) is the search engine for the same transcripts. It shares the same transcription JSON cache directory and the same RSS feed. It uses a separate Elasticsearch index (`podcast_segments`).
