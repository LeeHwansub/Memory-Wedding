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
 * Wedding highlight slideshow.
 * Renders each still to a fixed-length clip (Ken Burns + fade), then concatenates.
 * Avoids zoompan+xfade timestamp bugs that produced long black tails.
 */
@Slf4j
@Component
public class HighlightVideoComposer {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int FPS = 30;
    private static final double DEFAULT_HOLD_SECONDS = 3.2;
    private static final double FADE_SECONDS = 0.7;

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
     * @param imageFiles ordered stills (wedding narrative order)
     * @param secondsPerImage hold duration per slide
     */
    public byte[] composeSlideshow(List<Path> imageFiles, double secondsPerImage) throws IOException {
        if (imageFiles == null || imageFiles.isEmpty()) {
            throw new IllegalArgumentException("이미지가 없습니다.");
        }
        if (!isAvailable()) {
            throw new IllegalStateException("FFmpeg가 설치되어 있지 않습니다.");
        }

        double hold = secondsPerImage > 0 ? secondsPerImage : DEFAULT_HOLD_SECONDS;
        double fade = Math.min(FADE_SECONDS, hold / 2.5);
        int frames = Math.max(1, (int) Math.round(hold * FPS));
        double exactDuration = frames / (double) FPS;

        Path workDir = Files.createTempDirectory("mw-highlight-");
        try {
            List<Path> clips = new ArrayList<>();
            for (int i = 0; i < imageFiles.size(); i++) {
                Path src = imageFiles.get(i);
                Path img = workDir.resolve(String.format(Locale.ROOT, "img-%03d.%s", i,
                        extension(src.getFileName().toString())));
                Files.copy(src, img);
                Path clip = workDir.resolve(String.format(Locale.ROOT, "clip-%03d.mp4", i));
                renderClip(img, clip, frames, exactDuration, fade, i % 2 == 0);
                clips.add(clip);
            }

            Path output = workDir.resolve("highlight.mp4");
            concatClips(clips, output, exactDuration * clips.size());

            long expectedMs = Math.round(exactDuration * clips.size() * 1000);
            log.info("Highlight composed: {} clips, expected ~{} ms, file {} bytes",
                    clips.size(), expectedMs, Files.size(output));
            return Files.readAllBytes(output);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("FFmpeg interrupted", e);
        } finally {
            deleteRecursively(workDir);
        }
    }

    private void renderClip(
            Path image,
            Path output,
            int frames,
            double duration,
            double fade,
            boolean zoomIn) throws IOException, InterruptedException {
        // Scale up slightly so zoompan can pan without black edges, then Ken Burns.
        String zoomExpr = zoomIn
                ? "min(1.0+0.0012*on,1.10)"
                : "if(lte(on,1),1.10,max(1.10-0.0012*on,1.0))";
        double fadeOutStart = Math.max(0, duration - fade);
        String vf = String.format(Locale.ROOT,
                "scale=%d:%d:force_original_aspect_ratio=increase,"
                        + "crop=%d:%d,"
                        + "zoompan=z='%s':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)'"
                        + ":d=%d:s=%dx%d:fps=%d,"
                        + "fps=%d,format=yuv420p,setsar=1,setpts=PTS-STARTPTS,"
                        + "fade=t=in:st=0:d=%.2f,fade=t=out:st=%.2f:d=%.2f",
                WIDTH + 80, HEIGHT + 45,
                WIDTH + 80, HEIGHT + 45,
                zoomExpr,
                frames, WIDTH, HEIGHT, FPS,
                FPS,
                fade, fadeOutStart, fade);

        List<String> command = List.of(
                "ffmpeg", "-y",
                "-loop", "1",
                "-framerate", String.valueOf(FPS),
                "-t", String.format(Locale.ROOT, "%.3f", duration),
                "-i", image.toAbsolutePath().toString(),
                "-vf", vf,
                "-frames:v", String.valueOf(frames),
                "-t", String.format(Locale.ROOT, "%.3f", duration),
                "-c:v", "libx264",
                "-preset", "veryfast",
                "-pix_fmt", "yuv420p",
                "-an",
                output.toAbsolutePath().toString()
        );

        runFfmpeg(command, output.getParent(), 90, "clip render");
    }

    private void concatClips(List<Path> clips, Path output, double totalDuration)
            throws IOException, InterruptedException {
        Path listFile = output.getParent().resolve("concat.txt");
        StringBuilder list = new StringBuilder();
        for (Path clip : clips) {
            list.append("file '").append(clip.toAbsolutePath()).append("'\n");
        }
        Files.writeString(listFile, list.toString());

        // Re-encode on concat so timestamps stay continuous (copy can preserve bad PTS).
        List<String> command = List.of(
                "ffmpeg", "-y",
                "-f", "concat",
                "-safe", "0",
                "-i", listFile.toAbsolutePath().toString(),
                "-t", String.format(Locale.ROOT, "%.3f", totalDuration + 0.05),
                "-c:v", "libx264",
                "-preset", "veryfast",
                "-pix_fmt", "yuv420p",
                "-movflags", "+faststart",
                "-an",
                "-r", String.valueOf(FPS),
                output.toAbsolutePath().toString()
        );

        runFfmpeg(command, output.getParent(), 120, "concat");
    }

    private void runFfmpeg(List<String> command, Path workDir, int timeoutSec, String label)
            throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .directory(workDir.toFile())
                .start();
        boolean finished = process.waitFor(timeoutSec, TimeUnit.SECONDS);
        String logTail = new String(process.getInputStream().readAllBytes());
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("FFmpeg " + label + " timed out");
        }
        if (process.exitValue() != 0) {
            log.warn("FFmpeg {} failed: {}", label, logTail.length() > 800
                    ? logTail.substring(logTail.length() - 800)
                    : logTail);
            throw new IOException("FFmpeg " + label + " failed (exit " + process.exitValue() + ")");
        }
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return "jpg";
        }
        String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        return switch (ext) {
            case "png", "jpeg", "jpg", "webp" -> ext.equals("jpeg") ? "jpg" : ext;
            default -> "jpg";
        };
    }

    private void deleteRecursively(Path root) {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best-effort
                }
            });
        } catch (IOException ignored) {
            // best-effort
        }
    }
}
