package net.droth.pufoquote.adapter.out.redis;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import net.droth.pufoquote.domain.port.out.VoteRepositoryPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class RedisVoteAdapter implements VoteRepositoryPort {

  private static final String LEADERBOARD_KEY = "votes:leaderboard";

  private final RedisTemplate<String, String> redisTemplate;

  @Override
  public void incrementVote(String quoteId) {
    redisTemplate.opsForZSet().incrementScore(LEADERBOARD_KEY, quoteId, 1.0);
  }

  @Override
  public void decrementVote(String quoteId) {
    Double newScore = redisTemplate.opsForZSet().incrementScore(LEADERBOARD_KEY, quoteId, -1.0);
    if (newScore != null && newScore <= 0) {
      redisTemplate.opsForZSet().remove(LEADERBOARD_KEY, quoteId);
    }
  }

  @Override
  public long getVoteCount(String quoteId) {
    Double score = redisTemplate.opsForZSet().score(LEADERBOARD_KEY, quoteId);
    return score == null ? 0L : score.longValue();
  }

  @Override
  public List<String> getTopVotedQuoteIds(int limit) {
    Set<String> result = redisTemplate.opsForZSet().reverseRange(LEADERBOARD_KEY, 0, limit - 1L);
    return result == null ? List.of() : List.copyOf(result);
  }
}
