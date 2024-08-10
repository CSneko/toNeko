package org.cneko.toneko.common.quirks;

public abstract class Quirk {
    private final String id;
    public Quirk(String id){
        this.id = id;
    }

    /**
     * 获取id
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * 获取交互值,当玩家与猫娘互动时应该增加的好感经验
     * @return 交互值
     */
    abstract public int getInteractionValue();

    @Override
    public String toString(){
        return id;
    }
}
