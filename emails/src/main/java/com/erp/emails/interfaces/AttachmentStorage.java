package com.erp.emails.interfaces;

public interface AttachmentStorage {
    StoredObject save(String filename, String contentType, byte[] bytes);
    void delete(String storageRef);

    record StoredObject(String storageRef, long size, String sha256) {}
}
