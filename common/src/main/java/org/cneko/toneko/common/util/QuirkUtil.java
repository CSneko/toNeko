package org.cneko.toneko.common.util;

import org.cneko.toneko.common.mod.quirks.Quirk;

import java.util.Collection;
import java.util.List;

public class QuirkUtil {
    public static List<String> quirkToIds(Collection<Quirk> quirks){
        return quirks.stream().map(Quirk::getId).toList();
    }
}
