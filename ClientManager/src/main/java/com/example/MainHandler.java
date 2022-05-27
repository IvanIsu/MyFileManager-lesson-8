package com.example;

import dto.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;


import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;


public class MainHandler extends ChannelInboundHandlerAdapter {


    private static HashMap<String, FileOutputStream> writeFiles = new HashMap<>();
    private String token;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        BasicResponse response = (BasicResponse) msg;

        if(response instanceof AuthResponse) {
            AuthResponse authResponse = (AuthResponse) msg;
            String[] tmp = authResponse.getResponse().split(" ");

            if (tmp[0].equals("authOk")) {
                Network.setToken(tmp[1]);
                AuthController.switchToMain();

            } else if (tmp[0].equals("authFail")) {
                AuthController.showAlertMsg("authFail");
            } else if (tmp[0].equals("RegOk")) {
                AuthController.showDoneMsg("RegOK");
            } else if (tmp[0].equals("RegFail")) {
                AuthController.showAlertMsg("RegFail");
            }
        }else if (response instanceof UpdateFIleListResponse){
            UpdateFIleListResponse updateFIleListResponse = (UpdateFIleListResponse)msg;
            String nickName = updateFIleListResponse.getResponse();
            String path = updateFIleListResponse.getPath();
            List fileList = updateFIleListResponse.getFileInfoList();
            long totalSize = updateFIleListResponse.getTotalSize();

            PanelServerController.updateFileList(nickName, path, fileList, totalSize);

        }else if (response instanceof FileDownloadResponse){
            FileDownloadResponse fileDownloadResponse = (FileDownloadResponse) response;
            copyFileOnClient(fileDownloadResponse);
        }else if(response instanceof EndFileDownLoadResponse){
            EndFileDownLoadResponse endFileDownLoadResponse = (EndFileDownLoadResponse)response;
            writeFiles.get(endFileDownLoadResponse.getResponse()).close();
            writeFiles.remove(endFileDownLoadResponse.getResponse());
            MainController.updateTable();
        }else if(response instanceof BasicResponse){
            AuthController.showAlertMsg(response.getResponse());
        }


    }
    public static void copyFileOnClient(FileDownloadResponse fileDownloadResponse) throws IOException {
        FileOutputStream fileOutputStream = null;
        FileChannel fileChannel;
        if(writeFiles.isEmpty()){
            Path path = Paths.get(fileDownloadResponse.getDstPath());
            fileOutputStream = new FileOutputStream(path.toFile(),true);
            fileChannel = fileOutputStream.getChannel();
            writeFiles.put(fileDownloadResponse.getResponse(),fileOutputStream);
            ByteBuffer buffer = ByteBuffer.allocate(15*1_000_000);
            buffer.put(fileDownloadResponse.getBytes());
            buffer.flip();
            while (buffer.hasRemaining()){
                fileChannel.write(buffer);
            }
            buffer.clear();
        }else {
            fileChannel = writeFiles.get(fileDownloadResponse.getResponse()).getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(15*1_000_000);
            buffer.put(fileDownloadResponse.getBytes());
            buffer.flip();
            while (buffer.hasRemaining()){
                fileChannel.write(buffer);
            }

            buffer.clear();

        }
    }
}
