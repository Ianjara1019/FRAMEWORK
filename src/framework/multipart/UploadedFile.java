package framework.multipart;

import java.util.Arrays;

public final class UploadedFile {
    private final String originalName;
    private final String extension;
    private final String contentType;
    private final long size;
    private final byte[] bytes;

    public UploadedFile(String originalName, String extension, String contentType, byte[] bytes) {
        this.originalName = originalName;
        this.extension = extension;
        this.contentType = contentType;
        this.bytes = bytes == null ? new byte[0] : Arrays.copyOf(bytes, bytes.length);
        this.size = this.bytes.length;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getExtension() {
        return extension;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public byte[] getBytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }
}