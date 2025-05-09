package org.cneko.gal.common.util;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

public class FileUtil {
    public static InputStream readFile(File file) {
        try {
            return file.toURI().toURL().openStream();
        } catch (Exception e) {
            return null;
        }
    }
}
