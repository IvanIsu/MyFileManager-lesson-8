package dto;

import lombok.Data;

@Data public class FileUploadRequest extends BasicRequest {
    private String srcPath;
    private String dstPath;
    private String fileName;

    public FileUploadRequest(String token, String srcPath, String dstPath, String fileName) {
        super(token);
        this.srcPath = srcPath;
        this.dstPath = dstPath;
        this.fileName = fileName;
    }


}
