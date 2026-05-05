package net.droth.pufoquote.domain.port.out;

import java.nio.file.Path;
import java.util.List;
import net.droth.pufoquote.domain.model.Segment;

/** Output port for transcribing audio files into segments. */
public interface TranscriptionPort {
  List<Segment> transcribe(Path mp3Path);
}
