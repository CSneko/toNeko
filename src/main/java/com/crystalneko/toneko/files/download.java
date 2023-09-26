package com.crystalneko.toneko.files;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class download {
    //下载插件
    public static void downloadFile(String fileUrl, String saveFilePath) throws IOException {
        URL url = new URL(fileUrl);
        URLConnection connection = url.openConnection();
        BufferedInputStream in = new BufferedInputStream(connection.getInputStream());

        File outputFile = new File(saveFilePath);
        File parentDir = outputFile.getParentFile();
        if (parentDir != null) {
            parentDir.mkdirs();
        }

        FileOutputStream out = new FileOutputStream(outputFile);

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }

        out.close();
        in.close();
    }
}
