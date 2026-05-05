# pufoquote

Random quote generator for [Das Podcast UFO](https://www.daspodcastufo.com/) — a German comedy podcast.
Automatically transcribes episodes, scores sentences with an LLM, and serves the best ones as shareable quotes.

## What it does

- Pulls new episodes from the RSS feed and transcribes them via [Groq Whisper](https://groq.com)
- Scores and categorizes each sentence with an LLM (funny, dramatic, interesting, serious, meta)
- Serves a random high-quality quote on every page load
- Filter by category, share a quote via URL, or expand the surrounding context
- Vote for quotes with a thumbs-up; the best-of page ranks them by vote count

## Stack

- **Backend**: Java 25, Spring Boot, Elasticsearch, Redis
- **Frontend**: Thymeleaf, vanilla JS (no framework, no bundler)
- **Transcription & categorization**: Groq API (Whisper + Llama)
- **Infrastructure**: Docker Compose, Caddy (TLS), GitHub Actions

## Running locally

**Prerequisites**: Java 25, Docker, `ffmpeg`, a [Groq API key](https://console.groq.com)

```bash
# 1. Start Elasticsearch and Redis
docker compose up elasticsearch redis -d

# 2. Copy and fill in the env file
cp .env.example .env
# edit .env: set GROQ_API_KEY and ADMIN_PASSWORD

# 3. Run the app
set -a && source .env && set +a && \
  mvn spring-boot:run
```

App starts at http://localhost:8080

To index episodes (requires the app to be running):

```bash
curl -u admin:$ADMIN_PASSWORD -X POST http://localhost:8080/admin/index-all
```

## Architecture

Hexagonal (Ports & Adapters). The domain has zero framework dependencies.

```
Adapters (in)       →  Use Cases       →  Domain Ports (out)  →  Adapters (out)
QuoteController        GetRandom           QuoteRepositoryPort    Elasticsearch
AdminController        GetById             VoteRepositoryPort     Redis
ScheduledIndexer       IndexEpisodes       TranscriptionPort      Groq Whisper
                       VoteForQuote        CategorizationPort     Groq Llama
                       GetBestOf           FeedPort               Acast RSS
```
