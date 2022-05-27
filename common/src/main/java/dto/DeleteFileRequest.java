package dto;

import lombok.Data;

@Data public class DeleteFileRequest extends BasicRequest {

    private String path;

    public DeleteFileRequest(String token, String path) {
        super(token);
        this.path = path;
    }


}
