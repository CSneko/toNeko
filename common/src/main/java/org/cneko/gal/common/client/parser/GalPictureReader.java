package org.cneko.gal.common.client.parser;

import org.cneko.gal.common.util.FileUtil;

import java.io.InputStream;
import java.nio.file.Path;

public class GalPictureReader {
    private final Path path;
    public GalPictureReader(Path path) {
        this.path = path;
    }

    public InputStream readStandPicture(String name){
        return FileUtil.readFile(path.resolve("stand_pictures").resolve(name+".png").toFile());
    }
}
