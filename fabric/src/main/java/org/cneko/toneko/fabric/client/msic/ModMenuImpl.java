package org.cneko.toneko.fabric.client.msic;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import org.cneko.toneko.common.mod.client.screens.ConfigScreen;

public class ModMenuImpl implements ModMenuApi{

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (ConfigScreen::new);
    }
}
