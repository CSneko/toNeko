package org.cneko.toneko.common.mod.client.music;

import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ToNekoMusic {
    // 《虫儿飞》
    public static final MusicRecord FIREFLIES = new MusicRecord(
            new int[]{
                    30,30,30,40,50,30,20,  10,10,10,20,30,30,7, // 黑黑的天空低垂，亮亮的繁星相随
                    6,30,20, 6,30,20, 6,30,20,10,10, // 虫儿飞，虫儿飞，你在思念谁
                    3,3,3,4,5,3,2, 1,1,1,2,3,3,7, // 天上的星星流泪，地上的玫瑰枯萎
                    6,30,20, 6,30,20, 6,30,20,10,10, // 冷风吹，冷风吹，只要有你陪
                    3,2,5, 4,3,2, 5,4,3,4,5,3,2, // 虫儿飞，花儿睡，一双又一对才美
                    10,6,30,20, 10,5,20,10, // 不怕天黑，只怕心碎
                    40,30,40,30,10,40,30,40,30,10,20,10 // 不管累不累，也不管东南西北
            },
            NoteBlockInstrument.HARP,
            400
    );
    // 《小星星》
    public static final MusicRecord LITTLE_STAR = new MusicRecord(
            new int[]{
                    1,1,5,5,6,6,5, 4,4,3,3,2,2,1,
                    5,5,4,4,3,3,2, 5,5,4,4,3,3,2,
                    1,1,5,5,6,6,5, 4,4,3,3,2,2,1
        },
        NoteBlockInstrument.HARP,
            500
    );
    // 《Counting Stars》
    public static final MusicRecord COUNTING_STARS = new MusicRecord(
            new int[]{
                    2,3,5,2,2,3,2,1,3, // Lately,I've been,I've been losing sleep
                    2,2,3,4,3,2,1, 3,-6,1, // Dreaming about the things that we could be
                    -6,2,3,5,3,2,3,2,1,3, // But baby,I've been,I've been praying hard
                    2,2,3,4,3,2,1,3,2,2,1,1,-6, // Said no more counting dollars,We'll be counting stars
                    -4,3,2,2,1,0,1,-6, //Yeah, we'll be counting stars
            },
            NoteBlockInstrument.PLING,
            200
    );
    // 《千本樱》
    public static final MusicRecord SENBON_ZAKURA = new MusicRecord(
            new int[]{
                    2,2,1,2,4,4,5, 2,2,1,2,1,-6,1,
                    2,2,1,2,4,5, 6,6,5,4,2,
                    2,2,1,2,4,4,5, 2,2,1,2,1,1,-6,
                    2,2,1,1,2,4,5, 6,5,4,2,
                    4,3,2,1,1,1,2,-6,-5,-6, -6,1,2,5,3, 4,3,1,2,
                    4,3,2,1, 1,1,2,-6,-5,-6, -6,1,2,2, 2,4,5,3,

                    2,4,5,5,6,6, 6,10,20,5,4,6,
                    2,4,5,5,6,6, 6,51,6,5,4,4,
                    2,4,5,5,6,6, 6,10,20,5,4,6,
                    2,4, 51,6,5,4, 5,6,3,1,2,
                    2,4,5,5,6,6, 6,10,20,5,4,6,
                    2,4,5,5,6,6, 6,51,6,5,4,4,
                    2,4,5,5,6,6, 6,10,20,5,4,6,
                    2,4, 51,6,5,4, 5,4,6,10,20
            },
            NoteBlockInstrument.PLING,
            100
    );

    public static final List<MusicRecord> MUSICS = new ArrayList<>(List.of(
            FIREFLIES,
            LITTLE_STAR,
            COUNTING_STARS,
            SENBON_ZAKURA
    ));

    public static final Map<Integer, Integer> NOTE_MAP = Map.ofEntries(
            Map.entry(0, 0),  // 休止符
            // 低音部分（低音1~7：-1 到 -7）
            Map.entry(-1, 60 - 12),  // 低1：48
            Map.entry(-2, 62 - 12),  // 低2：50
            Map.entry(-3, 64 - 12),  // 低3：52
            Map.entry(-4, 65 - 12),  // 低4：53
            Map.entry(-5, 67 - 12),  // 低5：55
            Map.entry(-6, 69 - 12),  // 低6：57
            Map.entry(-7, 71 - 12),  // 低7：59
            // 低音部分升号（低音1#~7#：-11 到 -71）
            Map.entry(-11, 49), // 低1#
            Map.entry(-21, 51), // 低2#
            Map.entry(-31, 53), // 低3#
            Map.entry(-41, 54), // 低4#
            Map.entry(-51, 56), // 低5#
            Map.entry(-61, 58), // 低6#
            // 中音部分（1~7）
            Map.entry(1, 60),
            Map.entry(2, 62),
            Map.entry(3, 64),
            Map.entry(4, 65),
            Map.entry(5, 67),
            Map.entry(6, 69),
            Map.entry(7, 71),
            // 中音部分升号（1#~7#：11 到 71）
            Map.entry(11, 61), // 1#
            Map.entry(21, 63), // 2#
            Map.entry(31, 65), // 3#
            Map.entry(41, 66), // 4#
            Map.entry(51, 68), // 5#
            Map.entry(61, 70), // 6#
            Map.entry(71, 72), // 7#
            // 高音部分（高音1~7：10~70）
            Map.entry(10, 60 + 12), // 高1：72
            Map.entry(20, 62 + 12), // 高2：74
            Map.entry(30, 64 + 12), // 高3：76
            Map.entry(40, 65 + 12), // 高4：77
            Map.entry(50, 67 + 12), // 高5：79
            Map.entry(60, 69 + 12), // 高6：81
            Map.entry(70, 71 + 12),  // 高7：83
            // 高音部分升号（101~701）
            Map.entry(101, 73), // 高1#
            Map.entry(201, 75), // 高2#
            Map.entry(301, 77), // 高3#
            Map.entry(401, 78), // 高4#
            Map.entry(501, 80), // 高5#
            Map.entry(601, 82), // 高6#
            Map.entry(701, 84)  // 高7#
    );
}
