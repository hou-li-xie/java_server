package com.example.csv;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class Base64MultipartFile implements MultipartFile {
    private final byte[] fileContent;
    private final String fileName;

    public Base64MultipartFile(byte[] fileContent, String fileName) {
        this.fileContent = fileContent;
        this.fileName = fileName;
    }

    @Override
    public String getName() {
        return fileName;
    }

    @Override
    public String getOriginalFilename() {
        return fileName;
    }

    @Override
    public String getContentType() {
        if (fileName.endsWith(".csv")) {
            return "text/csv";
        }
        return "application/octet-stream";
    }

    @Override
    public boolean isEmpty() {
        return fileContent == null || fileContent.length == 0;
    }

    @Override
    public long getSize() {
        return fileContent.length;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return fileContent;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(fileContent);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        throw new UnsupportedOperationException("transferTo is not supported");
    }
}
