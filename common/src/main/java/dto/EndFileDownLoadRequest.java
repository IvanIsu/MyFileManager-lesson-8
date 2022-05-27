package dto;

import lombok.Data;

@Data public class EndFileDownLoadRequest extends BasicRequest {

    private String fileName;

    public EndFileDownLoadRequest(String token, String fileName) {
        super(token);
        this.fileName = fileName;
    }


}
