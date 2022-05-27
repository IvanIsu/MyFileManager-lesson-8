package com.example;



import dto.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;


import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;


public class Network {

private static Channel channel;
private final int MB_20 = 20 * 1_000_000;
private static String token;

    public static void setToken(String token) {
        Network.token = token;
    }

    public static Channel getChannel() {
        return channel;
    }

    public Network(){

    new Thread(()->{
        EventLoopGroup workerGrope = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        b.group(workerGrope)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {

                        socketChannel.pipeline().addLast(
                                new ObjectDecoder(MB_20, ClassResolvers.cacheDisabled(null)),
                                new ObjectEncoder(),
                                new MainHandler());
                    }
                });
        try {
            ChannelFuture future = b.connect("localhost", 6868).sync();
            channel = future.channel();
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            workerGrope.shutdownGracefully();
        }
    }).start();
}
    public void tryAuth(String login, int password){
        AuthRequest authRequest = new AuthRequest(null, login, password);
        channel.writeAndFlush(authRequest);

    }
    public void tryRegistration(String login, String nickName, int password){
        RegRequest regRequest = new RegRequest(null, login, nickName, password);
        channel.writeAndFlush(regRequest);
    }

public static void exitAction(){
    channel.writeAndFlush(new ExitRequest(null));
    channel.close();
}
    public static void deleteFile (String path){
        channel.writeAndFlush(new DeleteFileRequest(token, path));
    }
    public static void getFileList(String fileName,String path){
        channel.writeAndFlush(new GetFileListRequest(token, fileName, path));
    }

    public static void  fileUpload(String srcPath, String dstPath, String fileName){
        channel.writeAndFlush(new FileUploadRequest(token, srcPath, dstPath, fileName));
    }

    public static void getFileListUP(String fileName, String path){
        channel.writeAndFlush(new GetFileListRequest(token,null, path));
    }

    public static void createDirectory(String path){
        channel.writeAndFlush(new CreateDirectoryRequest(token, path));
    }

    public static void fileDownload(String srcPath, String dstPath, String fileName) throws IOException {
        long fileSize = Files.size(Paths.get(srcPath));
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

                    channel.writeAndFlush(new FileDownloadRequest(token, fileName,dstPath, bytes, fileSize));
                }else{
                    channel.writeAndFlush(new FileDownloadRequest(token, fileName,dstPath, buffer.array(),fileSize));
                }
                buffer.clear();

            }
            channel.writeAndFlush(new EndFileDownLoadRequest(token,fileName));
            channel.writeAndFlush(new GetFileListRequest(token,null, dstPath));
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
}
