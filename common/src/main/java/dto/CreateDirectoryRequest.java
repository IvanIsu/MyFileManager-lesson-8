package dto;


public class CreateDirectoryRequest extends BasicRequest{
private String path;


    public CreateDirectoryRequest(String token, String path) {
        super(token);
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
