package com.uk;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class BIOEchoServer {

    private static final int PORT = 8081;

    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        try {
            // 创建服务器Socket，绑定到指定端口
            serverSocket = new ServerSocket(PORT);
            System.out.println("Echo服务器已启动，监听端口: " + PORT);

            while (true) {
                // 阻塞等待客户端连接
                Socket clientSocket = serverSocket.accept();
                System.out.println("新客户端连接: " + clientSocket.getInetAddress().getHostAddress());

                // 为每个客户端连接创建一个新线程处理
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("服务器发生错误: " + e.getMessage());
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    System.err.println("关闭服务器Socket错误: " + e.getMessage());
                }
            }
        }
    }

    // 客户端处理类，实现Runnable接口
    private static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (InputStream in = clientSocket.getInputStream();
                 OutputStream out = clientSocket.getOutputStream()) {

                byte[] buffer = new byte[1024];
                int bytesRead;

                // 读取客户端发送的数据（阻塞操作）
                while ((bytesRead = in.read(buffer)) != -1) {
                    String message = new String(buffer, 0, bytesRead);
                    System.out.println("收到来自 " + clientSocket.getInetAddress().getHostAddress() +
                            " 的消息: " + message.trim());

                    // 将收到的数据回显给客户端
                    out.write(buffer, 0, bytesRead);
                    out.flush();

                    // 如果客户端发送"bye"，则关闭连接
                    if (message.trim().equalsIgnoreCase("bye")) {
                        break;
                    }
                }

                System.out.println("客户端 " + clientSocket.getInetAddress().getHostAddress() + " 已断开连接");
            } catch (IOException e) {
                System.err.println("处理客户端 " + clientSocket.getInetAddress().getHostAddress() +
                        " 时发生错误: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("关闭客户端Socket错误: " + e.getMessage());
                }
            }
        }
    }
}
