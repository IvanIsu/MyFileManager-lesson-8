package com.example;

import dto.*;
import dto.FileInfo;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.java.Log;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.stream.Collectors;


@Log public class MainServerHandler extends ChannelInboundHandlerAdapter {



    private static HashMap<String, FileOutputStream> writeFiles = new HashMap<>();
    private UserInfo userInfo;
    private static final Path PATH_SERVER_ROOT = Paths.get("ServerManager/src/main/java/com/example/server/root/users");
    private static final long MAX_SIZE = 100*1_000_000L;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.log(Level.INFO, "Client connection " + ctx.channel());
        System.out.println("Client connection " + ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        BasicRequest request = (BasicRequest) msg;


        if (request instanceof AuthRequest) {
            AuthRequest authRequest = (AuthRequest)msg;
            String login = authRequest.getLogin();
            int pass = authRequest.getPassword();
            userInfo = AuthService.getNickByLoginAndPassword(login,pass);
            if(userInfo != null){
                userInfo.setSessionToken(tokenGen());
                userInfo.setUserPathRoot(PATH_SERVER_ROOT.resolve(userInfo.getUserLogin()));
                Path path = userInfo.getUserPathRoot();

                ctx.writeAndFlush(new AuthResponse("authOk " + userInfo.getSessionToken()));
                ctx.writeAndFlush(new UpdateFIleListResponse(userInfo.getUserName(), null, availableTotalSpace(totalFileSize(path)), fileUpdateList(path)));

            }else {
                ctx.writeAndFlush(new AuthResponse("authFail"));
            }

        }else if(request instanceof RegRequest) {
            RegRequest regRequest = (RegRequest)msg;
            boolean regOk = AuthService.createNewUserInBase(regRequest.getLogin(),regRequest.getNickName(),regRequest.getPassword());
            if(regOk == true){
                Files.createDirectory(PATH_SERVER_ROOT.resolve(regRequest.getLogin()));
                ctx.writeAndFlush(new AuthResponse("RegOk"));
            }else {
                ctx.writeAndFlush(new AuthResponse("RegFail"));
            }


        }else if (request instanceof GetFileListRequest) {
            GetFileListRequest getFileListRequest = (GetFileListRequest)msg;
            if(!checkToken(getFileListRequest, userInfo)) {
                log.log(Level.WARNING,"Fake token " + ctx.channel());
                ctx.channel().close();
                return;
            }
            Path pathGet;
            if(getFileListRequest.getPath() != null){
                pathGet = userInfo.getUserPathRoot().resolve(getFileListRequest.getPath());
            }else {
                pathGet = userInfo.getUserPathRoot();
            }

            if(getFileListRequest.getFileName() != null){
                if(Files.isDirectory(pathGet)){
                    ctx.writeAndFlush(new UpdateFIleListResponse(null,
                            userInfo.getUserPathRoot().relativize(pathGet).toString(),
                              availableTotalSpace(totalFileSize(userInfo.getUserPathRoot())),
                            fileUpdateList(pathGet)));
                }
            }else if(getFileListRequest.getFileName() == null){

                if(pathGet.getParent() != null && !pathGet.getFileName().toString().equals(userInfo.getUserLogin()) ){
                    Path updatePath = pathGet.getParent();
                    ctx.writeAndFlush(new UpdateFIleListResponse(null,
                            userInfo.getUserPathRoot().relativize(updatePath).toString(),
                            availableTotalSpace(totalFileSize(userInfo.getUserPathRoot())),
                            fileUpdateList(updatePath)));
                }
            }


        }else if(request instanceof FileDownloadRequest){
            FileDownloadRequest fileDownloadRequest = (FileDownloadRequest) request;
            if(!checkToken(fileDownloadRequest, userInfo)) {
                log.log(Level.WARNING,"Fake token " + ctx.channel());
                ctx.channel().close();
                return;
            }
            if(availableTotalSpace(totalFileSize(userInfo.getUserPathRoot())) - fileDownloadRequest.getTotalFileSize() < 0){
                ctx.writeAndFlush(new BasicResponse("Free space is over"));
                writeFiles.clear();
                return;
            }
            fileUpload(fileDownloadRequest, userInfo);

        }else if(request instanceof FileUploadRequest) {
            FileUploadRequest fileUploadRequest = (FileUploadRequest)request;
            if(!checkToken(fileUploadRequest, userInfo)) {
                log.log(Level.WARNING,"Fake token " + ctx.channel());
                ctx.channel().close();
                return;
            }
            String srcPath = userInfo.getUserPathRoot().resolve(fileUploadRequest.getSrcPath()).toString();
            String dstPath = fileUploadRequest.getDstPath();
            String fileName = fileUploadRequest.getFileName();
            fileDownload(ctx, srcPath, dstPath, fileName);

        }else if(request instanceof EndFileDownLoadRequest){
            EndFileDownLoadRequest endFileDownLoadRequest = (EndFileDownLoadRequest) request;
            if(!writeFiles.isEmpty()){
                writeFiles.get(endFileDownLoadRequest.getFileName()).close();
                writeFiles.remove(endFileDownLoadRequest.getFileName());
            }

        }else if(request instanceof DeleteFileRequest){
         DeleteFileRequest deleteFileRequest = (DeleteFileRequest) request;
            if(!checkToken(deleteFileRequest, userInfo)) {
                log.log(Level.WARNING,"Fake token " + ctx.channel());
                ctx.channel().close();
                return;
            }
         Path path = userInfo.getUserPathRoot().resolve(deleteFileRequest.getPath());
         deleteFile(path);
         ctx.writeAndFlush(new UpdateFIleListResponse(null,
                 userInfo.getUserPathRoot().relativize(path.getParent()).toString(),
                 availableTotalSpace(totalFileSize(userInfo.getUserPathRoot())),
                 fileUpdateList(path.getParent())));
        }else if(request instanceof CreateDirectoryRequest){
            int count = 0;
            CreateDirectoryRequest createDirectoryRequest = (CreateDirectoryRequest)request;
            Path path;
            System.out.println(createDirectoryRequest.getPath());
            if(createDirectoryRequest.getPath() != null){
                path = userInfo.getUserPathRoot().resolve(createDirectoryRequest.getPath()).resolve("New Folder");
                System.out.println(path + "!=null");
            }else {
                path = userInfo.getUserPathRoot().resolve("New Folder");
                System.out.println(path + "==null");
            }
            while(Files.exists(path)){
                System.out.println(path + "while");
                path = path.getParent().resolve("New Folder " + count++);
            }
            System.out.println(path + "to create directory");
            System.out.println(path.getParent() + "getParent \n NEXT \n");
            Files.createDirectory(path);
            ctx.writeAndFlush(new UpdateFIleListResponse(null,
                    userInfo.getUserPathRoot().relativize(path.getParent()).toString(),
                    availableTotalSpace(totalFileSize(userInfo.getUserPathRoot())),
                    fileUpdateList(path.getParent())));

        }else if(request instanceof ExitRequest){
            ctx.close();
    }

    }

    public static void fileUpload(FileDownloadRequest fileDownloadRequest, UserInfo userInfo) throws IOException {
        FileOutputStream fileOutputStream = null;
        FileChannel fileChannel;
        if(writeFiles.isEmpty()){
            Path path = userInfo.getUserPathRoot().resolve(fileDownloadRequest.getDstPath());
            fileOutputStream = new FileOutputStream(path.toFile(),true);
            fileChannel = fileOutputStream.getChannel();
            writeFiles.put(fileDownloadRequest.getFileName(),fileOutputStream);
            ByteBuffer buffer = ByteBuffer.allocate(15*1_000_000);
            buffer.put(fileDownloadRequest.getBytes());
            buffer.flip();
            while (buffer.hasRemaining()){
                fileChannel.write(buffer);
            }
            buffer.clear();
        }else {
            fileChannel = writeFiles.get(fileDownloadRequest.getFileName()).getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(15*1_000_000);
            buffer.put(fileDownloadRequest.getBytes());
            buffer.flip();
            while (buffer.hasRemaining()){
                fileChannel.write(buffer);
            }

            buffer.clear();

        }

    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.log(Level.INFO, "Client disconnect: " + ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();

        log.log(Level.WARNING, cause.toString());
        ctx.close();
    }

    public static List<FileInfo> fileUpdateList(Path path) throws IOException {
        return Files.list(path).map(FileInfo::new).collect(Collectors.toList());
    }

    public static long totalFileSize(Path path) throws IOException {
      long finalTotalSize = 0L;
        finalTotalSize = Files.walk(path).mapToLong(item -> {
            try {
                return Files.size(item);
            } catch (IOException e) {
                e.printStackTrace();
            }return 0L;
        }).sum();
        return finalTotalSize;

    }

    public static void deleteFile(Path path){
        if(Files.isDirectory(path)){
            try {
                List<FileInfo> list = fileUpdateList(path);
                for (FileInfo o: list) {
                    Files.delete(path.resolve(o.getFileName()));
                }
                Files.delete(path);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }else {
            try {
                Files.delete(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static void fileDownload(ChannelHandlerContext channel, String srcPath, String dstPath, String fileName){
        int MB_15 = 15 * 1_000_000;
        FileChannel fileChannel = null;
        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(srcPath);
            fileChannel = fileInputStream.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(MB_15);
            while (fileChannel.read(buffer) > 0) {
                buffer.flip();
                if(fileChannel.position() == fileChannel.size()){
                    int lastRead = (int) (fileChannel.size() % MB_15);
                    byte[] bytes = new byte[lastRead];
                    buffer.get(bytes, 0, lastRead);

                    channel.writeAndFlush(new FileDownloadResponse(null,fileName,dstPath, bytes));
                }else{
                    channel.writeAndFlush(new FileDownloadResponse(null, fileName,dstPath, buffer.array()));
                }
                buffer.clear();

            }
            channel.writeAndFlush(new EndFileDownLoadResponse(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                fileChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


    public static String tokenGen(){
        byte[] abc = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes(StandardCharsets.UTF_8);
        Random random = new Random();
        StringBuilder st = new StringBuilder();
        for(int i = 0; i !=25; i++){
            st.append (((char) abc[random.nextInt(abc.length)]));
        }
        return st.toString();
    }



    public static boolean checkToken(BasicRequest request, UserInfo userInfo){
        if(request.getToken().equals(userInfo.getSessionToken())){
            return true;
        }
        return false;
    }


    private long availableTotalSpace(long totalSize){
        return MAX_SIZE - totalSize;
    }

}
