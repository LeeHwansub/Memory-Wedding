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
 * 웨딩 하이라이트: 사진(줌) 클립 + 영상 앞부분 클립을 만든 뒤 이어 붙입니다.
 * zoompan+xfade 한 그래프는 타임스탬프 꼬임으로 긴 검은 화면이 나와, 클립 단위 합성 후 concat 합니다.
 */
@Slf4j
@Component
public class HighlightVideoComposer {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int FPS = 30;
    private static final double DEFAULT_PHOTO_SECONDS = 3.2;
    private static final double DEFAULT_VIDEO_SECONDS = 4.0;
    private static final double FADE_SECONDS = 0.7;

    public enum SegmentKind {
        PHOTO,
        VIDEO
    }

    public record Segment(SegmentKind kind, Path source) {
    }

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

    /** 사진만으로 슬라이드쇼 (하위 호환). */
    public byte[] composeSlideshow(List<Path> imageFiles, double secondsPerImage) throws IOException {
        if (imageFiles == null || imageFiles.isEmpty()) {
            throw new IllegalArgumentException("이미지가 없습니다.");
        }
        List<Segment> segments = imageFiles.stream()
                .map(path -> new Segment(SegmentKind.PHOTO, path))
                .toList();
        return composeSegments(segments, secondsPerImage, DEFAULT_VIDEO_SECONDS);
    }

    /**
     * 사진·영상을 예식 흐름 순으로 합성합니다.
     * 각 클립을 1280×720 · 30fps · 무음으로 맞춘 뒤 concat 합니다.
     */
    public byte[] composeSegments(
            List<Segment> segments,
            double secondsPerPhoto,
            double secondsPerVideo) throws IOException {
        if (segments == null || segments.isEmpty()) {
            throw new IllegalArgumentException("하이라이트 클립이 없습니다.");
        }
        if (!isAvailable()) {
            throw new IllegalStateException("FFmpeg가 설치되어 있지 않습니다.");
        }

        double photoHold = secondsPerPhoto > 0 ? secondsPerPhoto : DEFAULT_PHOTO_SECONDS;
        double videoHold = secondsPerVideo > 0 ? secondsPerVideo : DEFAULT_VIDEO_SECONDS;
        double photoFade = Math.min(FADE_SECONDS, photoHold / 2.5);
        double videoFade = Math.min(FADE_SECONDS, videoHold / 2.5);
        int photoFrames = Math.max(1, (int) Math.round(photoHold * FPS));
        double photoDuration = photoFrames / (double) FPS;

        Path workDir = Files.createTempDirectory("mw-highlight-");
        try {
            List<Path> clips = new ArrayList<>();
            double totalDuration = 0;
            for (int i = 0; i < segments.size(); i++) {
                Segment segment = segments.get(i);
                Path clip = workDir.resolve(String.format(Locale.ROOT, "clip-%03d.mp4", i));
                if (segment.kind() == SegmentKind.PHOTO) {
                    Path img = workDir.resolve(String.format(Locale.ROOT, "img-%03d.%s", i,
                            extension(segment.source().getFileName().toString())));
                    Files.copy(segment.source(), img);
                    renderPhotoClip(img, clip, photoFrames, photoDuration, photoFade, i % 2 == 0);
                    totalDuration += photoDuration;
                } else {
                    double videoDuration = renderVideoClip(
                            segment.source(), clip, videoHold, videoFade);
                    totalDuration += videoDuration;
                }
                clips.add(clip);
            }

            Path output = workDir.resolve("highlight.mp4");
            concatClips(clips, output, totalDuration);

            log.info("Highlight composed: {} clips (mixed), expected ~{} ms, file {} bytes",
                    clips.size(), Math.round(totalDuration * 1000), Files.size(output));
            return Files.readAllBytes(output);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("FFmpeg interrupted", e);
        } finally {
            deleteRecursively(workDir);
        }
    }

    private void renderPhotoClip(
            Path image,
            Path output,
            int frames,
            double duration,
            double fade,
            boolean zoomIn) throws IOException, InterruptedException {
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

        runFfmpeg(command, output.getParent(), 90, "photo clip");
    }

    /** 영상 앞부분을 잘라 1280×720·30fps·무음으로 맞춥니다. 실제 사용 길이를 반환합니다. */
    private double renderVideoClip(
            Path video,
            Path output,
            double maxSeconds,
            double fade) throws IOException, InterruptedException {
        double duration = Math.max(1.0, maxSeconds);
        int frames = Math.max(1, (int) Math.round(duration * FPS));
        double exactDuration = frames / (double) FPS;
        double fadeOutStart = Math.max(0, exactDuration - fade);
        String vf = String.format(Locale.ROOT,
                "scale=%d:%d:force_original_aspect_ratio=decrease,"
                        + "pad=%d:%d:(ow-iw)/2:(oh-ih)/2,"
                        + "fps=%d,format=yuv420p,setsar=1,setpts=PTS-STARTPTS,"
                        + "fade=t=in:st=0:d=%.2f,fade=t=out:st=%.2f:d=%.2f",
                WIDTH, HEIGHT, WIDTH, HEIGHT, FPS,
                fade, fadeOutStart, fade);

        List<String> command = List.of(
                "ffmpeg", "-y",
                "-ss", "0",
                "-t", String.format(Locale.ROOT, "%.3f", exactDuration),
                "-i", video.toAbsolutePath().toString(),
                "-vf", vf,
                "-frames:v", String.valueOf(frames),
                "-t", String.format(Locale.ROOT, "%.3f", exactDuration),
                "-c:v", "libx264",
                "-preset", "veryfast",
                "-pix_fmt", "yuv420p",
                "-an",
                output.toAbsolutePath().toString()
        );

        runFfmpeg(command, output.getParent(), 120, "video clip");
        return exactDuration;
    }

    private void concatClips(List<Path> clips, Path output, double totalDuration)
            throws IOException, InterruptedException {
        Path listFile = output.getParent().resolve("concat.txt");
        StringBuilder list = new StringBuilder();
        for (Path clip : clips) {
            list.append("file '").append(clip.toAbsolutePath()).append("'\n");
        }
        Files.writeString(listFile, list.toString());

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

        runFfmpeg(command, output.getParent(), 180, "concat");
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
                    // 정리 실패는 무시
                }
            });
        } catch (IOException ignored) {
            // 정리 실패는 무시
        }
    }
}
