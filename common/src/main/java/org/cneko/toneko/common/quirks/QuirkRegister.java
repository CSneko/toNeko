package org.cneko.toneko.common.quirks;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class QuirkRegister {
    private static final Map<String, Quirk> quirks = new HashMap<>();

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
    public static Collection<Quirk> getQuirks(){
        return quirks.values();
    }

    /**
     * 获取所有性癖的id
     * @return id
     */
    @NotNull
    public static Collection<String> getQuirkIds(){
        return quirks.keySet();
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
