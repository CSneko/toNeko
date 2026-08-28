package org.cneko.toneko.common.mod.client.renderers;

import java.util.HashMap;
import java.util.Map;

/**
 * 非生物类 GeckoLib 实体（如弹药投掷物）的渲染状态。
 *
 * <h2>为何不能用 TonekoLivingEntityGeoState</h2>
 * GeckoLib 的 {@code GeoEntityRenderer#extractRenderState} 会检查
 * {@code entityRenderState instanceof LivingEntityRenderState} 来决定是否走
 * 生物状态提取，其中含 {@code (LivingEntity) entity} 强转。若给
 * 非生物实体（ThrowableProjectile 等）配 LivingEntityRenderState 系状态类，
 * 会触发 {@code ClassCastException}。此类继承 {@code EntityRenderState}，
 * 该检查不成立，走纯 GeckoLib 数据管线。
 */
public class TonekoGeoEntityState extends net.minecraft.client.renderer.entity.state.EntityRenderState
        implements com.geckolib.renderer.base.GeoRenderState {
    private final Map<com.geckolib.constant.dataticket.DataTicket<?>, Object> data = new HashMap<>();

    @Override
    public Map<com.geckolib.constant.dataticket.DataTicket<?>, Object> getDataMap() {
        return this.data;
    }

    /**
     * 26.1.2 GeckoLib 通过 EntityRenderStateMixin 给原版渲染状态注入了 @Unique 的
     * addGeckolibData（写入 mixin 私有 geckolib$data 表），而 getGeckolibData 走接口默认
     * 实现（经 getDataMap() 读取）。统一重写写入侧到自有 data 表，保证读写自洽。
     */
    @Override
    public <D> void addGeckolibData(com.geckolib.constant.dataticket.DataTicket<D> dataTicket, D data) {
        getDataMap().put(dataTicket, data);
    }

    @Override
    public boolean hasGeckolibData(com.geckolib.constant.dataticket.DataTicket<?> dataTicket) {
        return getDataMap().containsKey(dataTicket);
    }
}
