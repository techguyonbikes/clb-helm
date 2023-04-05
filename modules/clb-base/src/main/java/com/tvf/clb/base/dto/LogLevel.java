package com.tvf.clb.base.dto;

public enum LogLevel {
    INFO("info/"),
    WARN("info/"),
    ERROR("error/");

    private final String folder;

    LogLevel(String folder) {
        this.folder = folder;
    }

    public String getFolder() {
        return folder;
    }
}
