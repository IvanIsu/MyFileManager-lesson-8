package dto;

import lombok.Data;

@Data public class FileDownloadRequest extends BasicRequest {

    String fileName;
    String dstPath;
    byte[] bytes;
    long totalFileSize;




    public FileDownloadRequest(String token, String fileName, String dstPath, byte[] bytes, long totalFileSize) {
        super(token);
        this.bytes = bytes;
        this.fileName = fileName;
        this.dstPath = dstPath;
        this.totalFileSize = totalFileSize;
    }


}
