package org.cneko.toneko.common.mod.util;

public class RandomUtil {
    /**
     * 获取一个随机数
     * @param min 最小值
     * @param max 最大值
     * @return 随机数
     */
    public static int randomInt(int min, int max) {
        return (int) (Math.random() * (max - min + 1)) + min;
    }
}
