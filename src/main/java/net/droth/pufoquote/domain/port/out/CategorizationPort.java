package net.droth.pufoquote.domain.port.out;

import java.util.List;
import net.droth.pufoquote.domain.model.Classification;

/** Output port for batch-classifying sentences into categories using an LLM. */
public interface CategorizationPort {
  /**
   * Classifies a batch of sentences.
   *
   * <p>Returns a Classification (category + quality score 1–5) for each sentence in the same order
   * as the input. Never returns null; uses NONE/score=1 for failures.
   *
   * @param sentences the sentences to rate
   * @param contextBefore sentences immediately before this batch (shown to the model for context,
   *     not rated)
   * @param contextAfter sentences immediately after this batch (shown to the model for context, not
   *     rated)
   * @return classifications in the same order as the input sentences
   */
  List<Classification> classify(
      List<String> sentences, List<String> contextBefore, List<String> contextAfter);
}
