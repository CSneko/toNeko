package org.cneko.gal.common.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class FileUtil {
    public static InputStream readFile(File file) {
        try {
            return file.toURI().toURL().openStream();
        } catch (Exception e) {
            return null;
        }
    }

    public static Reader getFileReader(Path path) {
        try {
            return java.nio.file.Files.newBufferedReader(path);
        } catch (Exception e) {
            return null;
        }
    }

    public static String inputStreamToString(InputStream inputStream) {
        try {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }catch (Exception e){
            return null;
        }
    }

    /**
     * 从InputStream读取PNG图片的宽度和高度
     * @param is 包含PNG图片数据的输入流
     * @return 包含宽度和高度的数组，格式为[width, height]
     * @throws IOException 如果读取失败或不是有效的PNG文件
     */
    public static int[] getPngDimensions(InputStream is) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(is);

        // 检查PNG文件头签名 (8 bytes)
        byte[] signature = new byte[8];
        dataInputStream.readFully(signature);
        if (!isPngSignature(signature)) {
            throw new IOException("不是有效的PNG文件");
        }

        // 读取第一个数据块（应该是IHDR）
        int chunkLength = dataInputStream.readInt();
        byte[] chunkType = new byte[4];
        dataInputStream.readFully(chunkType);

        // 检查是否是IHDR块
        if (!"IHDR".equals(new String(chunkType))) {
            throw new IOException("PNG文件没有有效的IHDR块");
        }

        // 读取IHDR块内容：宽度(4 bytes)和高度(4 bytes)
        int width = dataInputStream.readInt();
        int height = dataInputStream.readInt();

        return new int[]{width, height};
    }

    /**
     * 检查字节数组是否符合PNG文件签名
     */
    private static boolean isPngSignature(byte[] bytes) {
        if (bytes == null || bytes.length < 8) {
            return false;
        }
        return bytes[0] == (byte) 137 &&
                bytes[1] == (byte) 80 &&
                bytes[2] == (byte) 78 &&
                bytes[3] == (byte) 71 &&
                bytes[4] == (byte) 13 &&
                bytes[5] == (byte) 10 &&
                bytes[6] == (byte) 26 &&
                bytes[7] == (byte) 10;
    }
}
