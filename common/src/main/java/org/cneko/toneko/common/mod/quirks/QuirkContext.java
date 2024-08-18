package org.cneko.toneko.common.mod.quirks;

import net.minecraft.world.entity.LivingEntity;

public record QuirkContext(LivingEntity entity) {

    public static class Builder {
        private final QuirkContext context;

        public Builder(QuirkContext context) {
            this.context = context;
        }

        public QuirkContext build() {
            return context;
        }

        /**
         * 创建一个QuirkContextBuilder
         *
         * @param entity 实体
         * @return Builder
         */
        public static Builder create(LivingEntity entity) {
            return new Builder(new QuirkContext(entity));
        }
    }
}
