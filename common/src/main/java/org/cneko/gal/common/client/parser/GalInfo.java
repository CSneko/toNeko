package org.cneko.gal.common.client.parser;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.cneko.gal.common.Gal;
import org.cneko.gal.common.util.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GalInfo {
    public static final Gson GSON = new Gson();
    @SerializedName("authors")
    private List<AuthorInfo> authors = new ArrayList<>();
    @SerializedName("plots")
    private List<PlotInfo> plots = new ArrayList<>();

    public List<AuthorInfo> getAuthors() {
        return authors;
    }
    public void setAuthors(List<AuthorInfo> authors) {
        this.authors = authors;
    }

    @NotNull
    public List<PlotInfo> getPlots() {
        return plots;
    }
    public void setPlots(List<PlotInfo> plots) {
        this.plots = plots;
    }

    public static class AuthorInfo {
        @SerializedName("name")
        private String name;
        @SerializedName("role")
        private String role;

        public String getName() {
            return name;
        }
        public List<String> getRole() {
            return List.of(role.split(","));
        }
        public String getRoleAsString() {
            return role;
        }
        public void setName(String name) {
            this.name = name;
        }
        public void setRole(String role) {
            this.role = role;
        }
    }

    public static class PlotInfo {
        @SerializedName("name")
        private String name;
        @SerializedName("desc")
        private String desc;

        public String getName() {
            return name;
        }
        public String getDesc() {
            return desc;
        }
        public void setName(String name) {
            this.name = name;
        }
        public void setDesc(String desc) {
            this.desc = desc;
        }
    }

    public static GalInfo parse(Path path) {
        try {
            System.out.println();
            return GSON.fromJson(FileUtil.inputStreamToString(FileUtil.readFile(path.toFile())), GalInfo.class);
        } catch (Exception e) {
            Gal.LOGGER.error("Failed to parse gal info", e);
            return null;
        }
    }

}
