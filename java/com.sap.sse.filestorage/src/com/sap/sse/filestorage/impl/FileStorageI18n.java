package com.sap.sse.filestorage.impl;

import java.nio.charset.StandardCharsets;

import com.sap.sse.i18n.ResourceBundleStringMessages;

public class FileStorageI18n {    
    private static final String RESOURCE_BASE_NAME = "stringmessages/FileStorageStringMessages";
    
    public static final ResourceBundleStringMessages STRING_MESSAGES = ResourceBundleStringMessages.create(
            RESOURCE_BASE_NAME, FileStorageI18n.class.getClassLoader(), StandardCharsets.UTF_8.name());
}
