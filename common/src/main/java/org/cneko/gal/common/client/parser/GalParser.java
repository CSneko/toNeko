package org.cneko.gal.common.client.parser;

import lombok.Getter;
import org.cneko.gal.common.util.FileUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GalParser {
    private static final String GAL_INFO_FILE_NAME = "main.json";
    private static final String PLOT_DIRECTORY_NAME = "plots";
    @Getter
    private final Path path;
    @Getter
    private final GalInfo galInfo;
    @Getter
    private final List<Map<String, PlotParser.DialogueNode>> plotParsers = new ArrayList<>();
    @Getter
    private final GalSoundPlayer soundPlayer;
    @Getter
    private final GalPictureReader pictureReader;
    public GalParser(Path path){
        this.path = path;
        // 解析GAL信息
        galInfo = GalInfo.parse(Path.of(path.toString(),GAL_INFO_FILE_NAME));
        // 获取所有的剧情文件
        for (GalInfo.PlotInfo plotInfo :galInfo.getPlots()){
            String name = plotInfo.getName();
            plotParsers.add(PlotParser.parseDialogueTree(
                    FileUtil.getFileReader(
                            Path.of(path.toString(),PLOT_DIRECTORY_NAME,name+".json"))
                    )
            );
        }
        soundPlayer = new GalSoundPlayer(Path.of(path.toString(),"sounds"));
        pictureReader = new GalPictureReader(Path.of(path.toString(),"pictures"));
    }
}
