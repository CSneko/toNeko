package org.cneko.gal.common.util;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
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
}
