package com.memorywedding.ai;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Extracts evenly spaced JPEG frames from a video using FFmpeg.
 * Returns empty list when FFmpeg is missing or extraction fails.
 */
@Slf4j
@Component
public class VideoFrameExtractor {

    public boolean isAvailable() {
        try {
            Process process = new ProcessBuilder("ffmpeg", "-version")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @param videoBytes raw video file bytes
     * @param frameCount desired number of frames (clamped to at least 1)
     * @return JPEG frame bytes (may be fewer than frameCount)
     */
    public List<byte[]> extractFrames(byte[] videoBytes, int frameCount) {
        if (videoBytes == null || videoBytes.length == 0) {
            return List.of();
        }
        if (!isAvailable()) {
            log.warn("FFmpeg not available; skipping video frame extraction");
            return List.of();
        }

        int count = Math.max(1, frameCount);
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("mw-video-frames-");
            Path input = workDir.resolve("input.bin");
            Files.write(input, videoBytes);

            double durationSec = probeDurationSeconds(input);
            if (durationSec <= 0) {
                durationSec = Math.max(1.0, count);
            }

            // fps so that ffmpeg emits ~count frames across the clip
            double fps = count / durationSec;
            if (fps <= 0) {
                fps = 1.0 / durationSec;
            }

            Path pattern = workDir.resolve("frame-%03d.jpg");
            Process extract = new ProcessBuilder(
                            "ffmpeg",
                            "-y",
                            "-i",
                            input.toAbsolutePath().toString(),
                            "-vf",
                            String.format(Locale.ROOT, "fps=%f", fps),
                            "-frames:v",
                            String.valueOf(count),
                            "-q:v",
                            "3",
                            pattern.toAbsolutePath().toString())
                    .redirectErrorStream(true)
                    .directory(workDir.toFile())
                    .start();
            boolean finished = extract.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                extract.destroyForcibly();
                log.warn("FFmpeg frame extract timed out");
                return List.of();
            }
            if (extract.exitValue() != 0) {
                log.warn("FFmpeg frame extract exited with {}", extract.exitValue());
            }

            List<Path> frames;
            try (Stream<Path> stream = Files.list(workDir)) {
                frames = stream
                        .filter(p -> p.getFileName().toString().startsWith("frame-")
                                && p.getFileName().toString().endsWith(".jpg"))
                        .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                        .limit(count)
                        .toList();
            }

            List<byte[]> result = new ArrayList<>();
            for (Path frame : frames) {
                result.add(Files.readAllBytes(frame));
            }
            return result;
        } catch (Exception e) {
            log.warn("Video frame extraction failed: {}", e.getMessage());
            return List.of();
        } finally {
            if (workDir != null) {
                deleteRecursively(workDir);
            }
        }
    }

    private double probeDurationSeconds(Path input) {
        try {
            Process probe = new ProcessBuilder(
                            "ffprobe",
                            "-v",
                            "error",
                            "-show_entries",
                            "format=duration",
                            "-of",
                            "default=noprint_wrappers=1:nokey=1",
                            input.toAbsolutePath().toString())
                    .redirectErrorStream(true)
                    .start();
            boolean finished = probe.waitFor(15, TimeUnit.SECONDS);
            if (!finished) {
                probe.destroyForcibly();
                return -1;
            }
            String out = new String(probe.getInputStream().readAllBytes()).trim();
            if (out.isBlank()) {
                return -1;
            }
            return Double.parseDouble(out.split("\\s+")[0]);
        } catch (Exception e) {
            return -1;
        }
    }

    private void deleteRecursively(Path root) {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best-effort cleanup
                }
            });
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }
}
