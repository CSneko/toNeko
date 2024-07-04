package org.cneko.toneko.common.api.quirk;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class QuirkRegister {
    private static Map<String, Quirk> quirks;

    /**
     * 注册一个性癖
     * @param quirk 性癖
     */
    public static void register(@NotNull Quirk quirk){
        quirks.put(quirk.getId(), quirk);
    }

    /**
     * 取消注册一个性癖
     * @param quirk 性癖
     */
    public static void unregister(@NotNull Quirk quirk){
        quirks.remove(quirk.getId());
    }

    /**
     * 从id获取性癖
     * @param id id
     * @return 性癖
     */
    @Nullable
    public static Quirk getById(String id){
        return quirks.get(id);
    }

    /**
     * 获取所有性癖
     * @return 性癖
     */
    @NotNull
    public static List<Quirk> getQuirks(){
        return (List<Quirk>) quirks.values();
    }

    /**
     * 是否存在性癖
     * @param id id
     * @return 是否存在
     */
    public static boolean hasQuirk(String id){
        return quirks.containsKey(id);
    }
}
