package com.uk;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NIOEchoServer {
    // 缓冲区大小
    private static final int BUFFER_SIZE = 1024;
    // 监听端口
    private static final int PORT = 8082;

    public static void main(String[] args) {
        try {
            // 创建选择器
            Selector selector = Selector.open();

            // 创建服务器通道
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            // 绑定端口
            serverSocketChannel.socket().bind(new InetSocketAddress(PORT));
            // 设置为非阻塞模式
            serverSocketChannel.configureBlocking(false);
            // 注册到选择器，关注连接事件
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("NIO Echo服务器已启动，监听端口: " + PORT);

            while (true) {
                // 阻塞等待就绪的通道，返回就绪通道的数量
                int readyChannels = selector.select();

                // 如果没有就绪的通道，继续等待
                if (readyChannels == 0) {
                    continue;
                }

                // 获取所有就绪通道的SelectionKey
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();

                    // 处理连接事件
                    if (key.isAcceptable()) {
                        handleAccept(key, selector);
                    }

                    // 处理读事件
                    if (key.isReadable()) {
                        handleRead(key);
                    }

                    // 处理写事件
                    if (key.isWritable()) {
                        handleWrite(key);
                    }

                    // 移除已处理的SelectionKey
                    keyIterator.remove();
                }
            }
        } catch (IOException e) {
            System.err.println("服务器发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 处理连接事件
    private static void handleAccept(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        // 接受客户端连接
        SocketChannel socketChannel = serverSocketChannel.accept();
        System.out.println("新客户端连接: " + socketChannel.getRemoteAddress());

        // 设置为非阻塞模式
        socketChannel.configureBlocking(false);

        // 为客户端通道注册读事件
        socketChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(BUFFER_SIZE));
    }

    // 处理读事件
    private static void handleRead(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();

        // 读取数据
        int bytesRead = socketChannel.read(buffer);

        if (bytesRead == -1) {
            // 客户端关闭连接
            System.out.println("客户端断开连接: " + socketChannel.getRemoteAddress());
            socketChannel.close();
            key.cancel();
            return;
        }

        // 切换为读模式
        buffer.flip();

        // 将字节转换为字符串
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        String message = new String(bytes).trim();
        System.out.println("收到来自 " + socketChannel.getRemoteAddress() + " 的消息: " + message);

        // 如果客户端发送"bye"，则关闭连接
        if (message.equalsIgnoreCase("bye")) {
            socketChannel.write(ByteBuffer.wrap("再见!".getBytes()));
            System.out.println("客户端 " + socketChannel.getRemoteAddress() + " 已断开连接");
            socketChannel.close();
            key.cancel();
            return;
        }

        // 准备回显数据，切换为写模式
        buffer.rewind();

        // 重新注册通道，关注写事件
        key.interestOps(SelectionKey.OP_WRITE);
    }

    // 处理写事件
    private static void handleWrite(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();

        // 回显数据给客户端
        socketChannel.write(buffer);

        // 如果缓冲区中还有数据未写完，则继续关注写事件
        if (buffer.hasRemaining()) {
            return;
        }

        // 清空缓冲区，切换为读模式
        buffer.clear();

        // 重新注册通道，关注读事件
        key.interestOps(SelectionKey.OP_READ);
    }
}
    