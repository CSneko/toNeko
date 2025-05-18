package org.cneko.gal.common.client.parser;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import org.cneko.gal.common.Gal;
import org.cneko.gal.common.util.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Setter
public class GalInfo {
    public static final Gson GSON = new Gson();
    @Getter
    @SerializedName("authors")
    private List<AuthorInfo> authors = new ArrayList<>();
    @SerializedName("plots")
    private List<PlotInfo> plots = new ArrayList<>();

    @NotNull
    public List<PlotInfo> getPlots() {
        return plots;
    }

    @Setter
    public static class AuthorInfo {
        @Getter
        @SerializedName("name")
        private String name;
        @SerializedName("role")
        private String role;

        public List<String> getRole() {
            return List.of(role.split(","));
        }
        public String getRoleAsString() {
            return role;
        }
    }

    @Setter
    @Getter
    public static class PlotInfo {
        @SerializedName("name")
        private String name;
        @SerializedName("desc")
        private String desc;

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
