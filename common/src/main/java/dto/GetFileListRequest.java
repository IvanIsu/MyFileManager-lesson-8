package dto;

import lombok.Data;

@Data
public class GetFileListRequest extends BasicRequest {

    private String fileName;
    private String path;

    public GetFileListRequest(String token, String fileName, String path) {
        super(token);
        this.fileName = fileName;
        this.path = path;
    }


}
