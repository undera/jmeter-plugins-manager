package org.jmeterplugins.repository.cache;

import java.io.Serializable;

public class PluginsRepo implements Serializable {

    private final String repoJSON;
    private final long expirationTime;

    public PluginsRepo(String repoJSON, long expirationTime) {
        this.repoJSON = repoJSON;
        this.expirationTime = expirationTime;
    }

    public boolean isActual() {
        return expirationTime > System.currentTimeMillis();
    }

    public String getRepoJSON() {
        return repoJSON;
    }
}
