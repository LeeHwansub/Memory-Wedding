package com.memorywedding.ai;

/**
 * 하이라이트 합성 옵션 프리셋.
 */
public record HighlightOptions(
        Style style,
        Length length,
        boolean bgm,
        boolean subtitles
) {

    public enum Style {
        /** 기본 줌·페이드 */
        CLASSIC,
        /** 약한 줌, 부드러운 페이드 */
        SOFT,
        /** 느린 줌, 긴 페이드 */
        CINEMATIC
    }

    public enum Length {
        /** 짧은 클립 · 최대 6개 */
        SHORT,
        /** 기본 */
        MEDIUM,
        /** 긴 클립 */
        LONG
    }

    public static HighlightOptions defaults() {
        return new HighlightOptions(Style.CLASSIC, Length.MEDIUM, false, false);
    }

    public static HighlightOptions fromRequest(
            String style,
            String length,
            Boolean bgm,
            Boolean subtitles) {
        return new HighlightOptions(
                parseStyle(style),
                parseLength(length),
                Boolean.TRUE.equals(bgm),
                Boolean.TRUE.equals(subtitles));
    }

    private static Style parseStyle(String raw) {
        if (raw == null || raw.isBlank()) {
            return Style.CLASSIC;
        }
        try {
            return Style.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            return Style.CLASSIC;
        }
    }

    private static Length parseLength(String raw) {
        if (raw == null || raw.isBlank()) {
            return Length.MEDIUM;
        }
        try {
            return Length.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            return Length.MEDIUM;
        }
    }

    public double photoSeconds() {
        return switch (length) {
            case SHORT -> 2.2;
            case MEDIUM -> 3.2;
            case LONG -> 4.5;
        };
    }

    public double videoSeconds() {
        return switch (length) {
            case SHORT -> 3.0;
            case MEDIUM -> 4.0;
            case LONG -> 5.5;
        };
    }

    public int maxClips() {
        return switch (length) {
            case SHORT -> 6;
            case MEDIUM -> 20;
            case LONG -> 30;
        };
    }

    public double fadeSeconds(double hold) {
        double base = switch (style) {
            case CLASSIC -> 0.7;
            case SOFT -> 1.0;
            case CINEMATIC -> 1.2;
        };
        return Math.min(base, hold / 2.5);
    }

    /** Ken Burns 줌 강도 (초당 on 증가분 스케일). */
    public double zoomSpeed() {
        return switch (style) {
            case CLASSIC -> 0.0012;
            case SOFT -> 0.0006;
            case CINEMATIC -> 0.0009;
        };
    }

    public double zoomMax() {
        return switch (style) {
            case CLASSIC -> 1.10;
            case SOFT -> 1.05;
            case CINEMATIC -> 1.12;
        };
    }
}
