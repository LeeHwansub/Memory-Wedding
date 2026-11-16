package com.memorywedding.ai;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;

/**
 * 8×8 average hash (aHash). 유사 사진 판별용. 실패 시 null (판단 불가 → 유지).
 */
public final class PhotoAverageHash {

    private static final int SIZE = 8;
    /** Hamming 거리가 이 값 이하면 유사(중복)로 본다. */
    public static final int SIMILARITY_THRESHOLD = 6;

    private PhotoAverageHash() {
    }

    public static Long compute(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (source == null) {
                return null;
            }
            BufferedImage gray = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g = gray.createGraphics();
            g.drawImage(source.getScaledInstance(SIZE, SIZE, Image.SCALE_SMOOTH), 0, 0, null);
            g.dispose();

            long sum = 0;
            int[] pixels = new int[SIZE * SIZE];
            gray.getRaster().getPixels(0, 0, SIZE, SIZE, pixels);
            for (int pixel : pixels) {
                sum += pixel;
            }
            double mean = sum / (double) pixels.length;

            long hash = 0L;
            for (int i = 0; i < pixels.length; i++) {
                if (pixels[i] >= mean) {
                    hash |= 1L << i;
                }
            }
            return hash;
        } catch (Exception e) {
            return null;
        }
    }

    public static int hamming(long a, long b) {
        return Long.bitCount(a ^ b);
    }

    public static boolean similar(Long a, Long b) {
        if (a == null || b == null) {
            return false;
        }
        return hamming(a, b) <= SIMILARITY_THRESHOLD;
    }

    public static String toHex(long hash) {
        return String.format("%016x", hash);
    }

    public static Long fromHex(String hex) {
        if (hex == null || hex.isBlank()) {
            return null;
        }
        try {
            return Long.parseUnsignedLong(hex.trim(), 16);
        } catch (Exception e) {
            return null;
        }
    }
}
