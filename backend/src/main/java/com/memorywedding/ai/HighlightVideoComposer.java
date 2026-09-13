package com.memorywedding.ai;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 웨딩 하이라이트: 사진(줌) + 영상 앞부분 클립 합성.
 * 스타일·길이·장면 자막·선택적 BGM을 지원합니다.
 */
@Slf4j
@Component
public class HighlightVideoComposer {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int FPS = 30;

    public enum SegmentKind {
        PHOTO,
        VIDEO
    }

    public record Segment(SegmentKind kind, Path source, String sceneLabel) {
        public Segment(SegmentKind kind, Path source) {
            this(kind, source, null);
        }
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

    public byte[] composeSlideshow(List<Path> imageFiles, double secondsPerImage) throws IOException {
        if (imageFiles == null || imageFiles.isEmpty()) {
            throw new IllegalArgumentException("이미지가 없습니다.");
        }
        List<Segment> segments = imageFiles.stream()
                .map(path -> new Segment(SegmentKind.PHOTO, path, null))
                .toList();
        return composeSegments(segments, HighlightOptions.defaults());
    }

    public byte[] composeSegments(List<Segment> segments, HighlightOptions options) throws IOException {
        if (segments == null || segments.isEmpty()) {
            throw new IllegalArgumentException("하이라이트 클립이 없습니다.");
        }
        if (!isAvailable()) {
            throw new IllegalStateException("FFmpeg가 설치되어 있지 않습니다.");
        }
        HighlightOptions opts = options == null ? HighlightOptions.defaults() : options;

        double photoHold = opts.photoSeconds();
        double videoHold = opts.videoSeconds();
        double photoFade = opts.fadeSeconds(photoHold);
        double videoFade = opts.fadeSeconds(videoHold);
        int photoFrames = Math.max(1, (int) Math.round(photoHold * FPS));
        double photoDuration = photoFrames / (double) FPS;
        String fontFile = resolveFontFile();

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
                    renderPhotoClip(
                            img, clip, photoFrames, photoDuration, photoFade,
                            opts, i % 2 == 0, segment.sceneLabel(), fontFile);
                    totalDuration += photoDuration;
                } else {
                    double videoDuration = renderVideoClip(
                            segment.source(), clip, videoHold, videoFade,
                            segment.sceneLabel(), fontFile);
                    totalDuration += videoDuration;
                }
                clips.add(clip);
            }

            Path silent = workDir.resolve("highlight-silent.mp4");
            concatClips(clips, silent, totalDuration);

            Path output = workDir.resolve("highlight.mp4");
            if (opts.bgm()) {
                Path bgm = extractBundledBgm(workDir);
                if (bgm != null) {
                    muxBgm(silent, bgm, output, totalDuration);
                } else {
                    log.info("BGM 요청됐지만 classpath:bgm/default.mp3 가 없어 무음으로 저장합니다.");
                    Files.copy(silent, output);
                }
            } else {
                Files.copy(silent, output);
            }

            log.info("Highlight composed: {} clips, style={}, length={}, ~{} ms, {} bytes",
                    clips.size(), opts.style(), opts.length(),
                    Math.round(totalDuration * 1000), Files.size(output));
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
            HighlightOptions opts,
            boolean zoomIn,
            String sceneLabel,
            String fontFile) throws IOException, InterruptedException {
        double zoomSpeed = opts.zoomSpeed();
        double zoomMax = opts.zoomMax();
        String zoomExpr = zoomIn
                ? String.format(Locale.ROOT, "min(1.0+%.4f*on,%.2f)", zoomSpeed, zoomMax)
                : String.format(Locale.ROOT,
                        "if(lte(on,1),%.2f,max(%.2f-%.4f*on,1.0))", zoomMax, zoomMax, zoomSpeed);
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
        vf = appendDrawText(vf, sceneLabel, fontFile, duration);

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

    private double renderVideoClip(
            Path video,
            Path output,
            double maxSeconds,
            double fade,
            String sceneLabel,
            String fontFile) throws IOException, InterruptedException {
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
        vf = appendDrawText(vf, sceneLabel, fontFile, exactDuration);

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

    private String appendDrawText(String vf, String sceneLabel, String fontFile, double duration) {
        if (sceneLabel == null || sceneLabel.isBlank() || fontFile == null) {
            return vf;
        }
        String escaped = sceneLabel
                .replace("\\", "\\\\")
                .replace(":", "\\:")
                .replace("'", "\\'");
        double showFor = Math.min(2.0, Math.max(0.8, duration * 0.45));
        return vf + String.format(Locale.ROOT,
                ",drawtext=fontfile='%s':text='%s':fontsize=42:fontcolor=white:"
                        + "borderw=2:bordercolor=black@0.6:x=(w-text_w)/2:y=h-90:"
                        + "enable='between(t,0,%.2f)'",
                fontFile.replace("'", "\\'").replace(":", "\\:"),
                escaped,
                showFor);
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

    private void muxBgm(Path video, Path bgm, Path output, double totalDuration)
            throws IOException, InterruptedException {
        List<String> command = List.of(
                "ffmpeg", "-y",
                "-i", video.toAbsolutePath().toString(),
                "-stream_loop", "-1",
                "-i", bgm.toAbsolutePath().toString(),
                "-t", String.format(Locale.ROOT, "%.3f", totalDuration + 0.05),
                "-c:v", "copy",
                "-c:a", "aac",
                "-b:a", "192k",
                "-shortest",
                "-movflags", "+faststart",
                output.toAbsolutePath().toString()
        );
        runFfmpeg(command, output.getParent(), 120, "bgm mux");
    }

    private Path extractBundledBgm(Path workDir) {
        try {
            ClassPathResource resource = new ClassPathResource("bgm/default.mp3");
            if (!resource.exists()) {
                return null;
            }
            Path target = workDir.resolve("default.mp3");
            try (InputStream in = resource.getInputStream()) {
                Files.copy(in, target);
            }
            return Files.size(target) > 0 ? target : null;
        } catch (Exception e) {
            log.warn("BGM 리소스 로드 실패: {}", e.getMessage());
            return null;
        }
    }

    private String resolveFontFile() {
        List<String> candidates = List.of(
                "/usr/share/fonts/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/noto/NotoSansCJKkr-Regular.otf",
                "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
                "/System/Library/Fonts/AppleSDGothicNeo.ttc",
                "/System/Library/Fonts/Supplemental/AppleGothic.ttf",
                "/Library/Fonts/AppleSDGothicNeo.ttc"
        );
        for (String path : candidates) {
            if (Files.isRegularFile(Path.of(path))) {
                return path;
            }
        }
        log.warn("한글 자막용 폰트를 찾지 못했습니다. 장면 자막을 건너뜁니다.");
        return null;
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
