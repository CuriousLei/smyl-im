package cn.buptleida.niohdl.core;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ioArgs {
    private int limit = 256;
    private byte[] byteBuffer = new byte[256];
    private ByteBuffer buffer = ByteBuffer.wrap(byteBuffer);
    private String srcUid;

    public int read(SocketChannel channel) throws IOException {
        buffer.clear();
        return channel.read(buffer);
    }

    public int write(SocketChannel channel) throws IOException {
        return channel.write(buffer);
    }

    /**
     * 获取容量
     * @return
     */
    public int capacity(){
        return buffer.capacity();
    }
    /**
     * 从bytes中读到buffer
     *
     * @param bytes
     * @param offset
     * @return
     */
    public int readFrom(byte[] bytes, int offset) {
        int size = Math.min(bytes.length - offset, buffer.remaining());
        buffer.put(bytes, offset, size);
        return size;
    }

    /**
     * 从buffer中写数据入到bytes
     *
     * @param bytes
     * @param offset
     * @return
     */
    public int writeTo(byte[] bytes, int offset) {
        int size = Math.min(bytes.length - offset, buffer.remaining());
        buffer.get(bytes, offset, size);
        return size;
    }

    public void startWriting() {
        buffer.clear();
        //定义容纳区间
        buffer.limit(limit);
    }

    public void finishWriting() {
        buffer.flip();
    }


    /**
     * 从channel中读取数据
     *
     * @param channel
     * @return
     * @throws IOException
     */
    public int readFrom(SocketChannel channel) throws IOException {
        startWriting();

        int byteProduced = 0;
        //hasRemaining就是position<limit返回true
        while (buffer.hasRemaining()) {
            int len = channel.read(buffer);
            if (len < 0) {
                throw new EOFException();
            }
            byteProduced += len;
        }
        //System.out.println("position="+buffer.position());

        finishWriting();

        //System.out.println("收到长度："+byteProduced);
        return byteProduced;
    }


    /**
     * 往channel中写数据
     *
     * @param channel
     * @return
     * @throws IOException
     */
    public int writeTo(SocketChannel channel) throws IOException {
        int byteProduced = 0;
        while (buffer.hasRemaining()) {
            int len = channel.write(buffer);
            //System.out.println("发送长度"+len);
            if (len < 0) {
                throw new EOFException();
            }
            byteProduced += len;
        }
        return byteProduced;
    }

    public void writeLength(int total) {
        buffer.putInt(total);
    }

    public int readLength(){
        return buffer.getInt();
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public String getSrcUid() {
        return srcUid;
    }

    public void setSrcUid(String srcUid) {
        this.srcUid = srcUid;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public interface IoArgsEventListener{
        void onStarted(ioArgs args);

        void onCompleted(ioArgs args);
    }
}
