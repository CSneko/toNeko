package com.crystalneko.tonekonk.api;

import static org.cneko.ctlib.common.util.LocalDataBase.Connections.sqlite;

public class NekoSet {
    public static void setNeko(String name, String owner) {
        sqlite.saveData("neko", "name", name);
        sqlite.saveDataWhere("neko", "owner", "name", name, owner);
    }
    public static boolean isNeko(String name) {
        return sqlite.getColumnValue("neko","owner","name",name) !=null;
    }
}
