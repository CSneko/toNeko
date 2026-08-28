package org.cneko.toneko.common.mod.client.renderers;

import com.geckolib.constant.dataticket.DataTicket;

import java.util.HashMap;
import java.util.Map;

/**
 * 26.x GeckoLib 迁移：原版 {@link net.minecraft.client.renderer.entity.state.LivingEntityRenderState}
 * 不再实现 GeoRenderState（数据存取混入被移除），自定义渲染器必须提供同时满足
 * “R extends LivingEntityRenderState” 与 “R extends GeoRenderState” 的状态类型。
 */
public class TonekoLivingEntityGeoState extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState
        implements com.geckolib.renderer.base.GeoRenderState {
    private final Map<DataTicket<?>, Object> data = new HashMap<>();

    @Override
    public Map<DataTicket<?>, Object> getDataMap() {
        return this.data;
    }

    /**
     * 26.1.2 GeckoLib 通过 EntityRenderStateMixin 给原版渲染状态注入了 @Unique 的
     * addGeckolibData（写入 mixin 私有 geckolib$data 表），而 getGeckolibData 走接口默认
     * 实现（经 getDataMap() 读取）。若不在这里重写写入侧，就会「写进 mixin 表、读自有表」，
     * 导致 ANIMATABLE_MANAGER 等数据永远读不到（AnimationProcessor#requireNonNull 崩溃）。
     * 统一改写到自有 data 表，保证读经 {@link #getDataMap()} 的默认实现自洽。
     */
    @Override
    public <D> void addGeckolibData(DataTicket<D> dataTicket, D data) {
        getDataMap().put(dataTicket, data);
    }

    @Override
    public boolean hasGeckolibData(DataTicket<?> dataTicket) {
        return getDataMap().containsKey(dataTicket);
    }
}
