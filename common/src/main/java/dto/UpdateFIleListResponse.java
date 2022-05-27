package dto;


import java.util.List;

public class UpdateFIleListResponse extends BasicResponse {


    private List <FileInfo> fileInfoList;
    private String path;
    private long totalSize;

    public UpdateFIleListResponse(String response, String path, long totalSize, List<FileInfo> fileInfoList) {
        super(response);
        this.path = path;
        this.fileInfoList = fileInfoList;
        this.totalSize = totalSize;

    }

    public List<FileInfo> getFileInfoList() {
        return fileInfoList;
    }

    public String getPath() {
        return path;
    }

    public long getTotalSize() {
        return totalSize;
    }
}
