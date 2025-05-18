package org.cneko.gal.common.client.parser;

import org.cneko.gal.common.Gal;
import org.cneko.gal.common.util.FileUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public class GalPictureReader {
    private final Path path;
    public GalPictureReader(Path path) {
        this.path = path;
    }

    public Picture readStandPicture(String name) {
        InputStream is = FileUtil.readFile(path.resolve("stand_pictures").resolve(name+".png").toFile());

        try {
            if (is != null && is.markSupported()) {
                is.mark(Integer.MAX_VALUE);
            }
            int[] dimensions = FileUtil.getPngDimensions(is);
            if (is != null && is.markSupported()) {
                is.reset();
            }
            return new Picture(name, dimensions[0], dimensions[1], is);
        } catch (IOException e) {
            Gal.LOGGER.error("Failed to read stand picture", e);
        }

        return new Picture(name, 0, 0, is);
    }

    public Picture readBigPicture(String name) {
        InputStream is = FileUtil.readFile(path.resolve("big_pictures").resolve(name + ".png").toFile());

        try {
            if (is != null && is.markSupported()) {
                is.mark(Integer.MAX_VALUE);
            }
            int[] dimensions = FileUtil.getPngDimensions(is);
            if (is != null && is.markSupported()) {
                is.reset();
            }
            return new Picture(name, dimensions[0], dimensions[1], is);
        } catch (IOException e) {
            Gal.LOGGER.error("Failed to read big picture", e);
        }

        return new Picture(name, 0, 0, is);
    }

    /**
     * 一个图片信息
     * @param name 图片名
     * @param width 宽（为0时则读取失败）
     * @param height 高（为0时则读取失败）
     * @param stream 图片流
     */
    public record Picture(String name, int width, int height,InputStream stream) {
    }
}
