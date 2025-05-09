package org.cneko.gal.common.client.parser;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GalInfo {
    @SerializedName("authors")
    private List<AuthorInfo> authors;
    @SerializedName("plots")
    private List<PlotInfo> plots;

    public List<AuthorInfo> getAuthors() {
        return authors;
    }
    public void setAuthors(List<AuthorInfo> authors) {
        this.authors = authors;
    }
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

}
