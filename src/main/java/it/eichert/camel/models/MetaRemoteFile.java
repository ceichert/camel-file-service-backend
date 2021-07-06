package it.eichert.camel.models;

import lombok.Data;

import java.util.UUID;

@Data
public class MetaRemoteFile {
    private UUID uuid;
    private long timestamp;
    private String parentPath;
    private String filename;

    public MetaRemoteFile(String filename, String parentPath) {
        this.filename = filename;
        this.parentPath = parentPath;
        this.timestamp = System.currentTimeMillis();
        this.uuid = UUID.randomUUID();
    }
}
