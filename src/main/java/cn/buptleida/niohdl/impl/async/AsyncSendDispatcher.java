package cn.buptleida.niohdl.impl.async;

import cn.buptleida.niohdl.core.*;
import cn.buptleida.utils.CloseUtil;
import com.sun.corba.se.impl.protocol.giopmsgheaders.IORAddressingInfo;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncSendDispatcher implements SendDispatcher {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private Sender sender;
    private Queue<SendPacket> queue = new ConcurrentLinkedDeque<>();
    private AtomicBoolean isSending = new AtomicBoolean();
    private ioArgs ioArgs = new ioArgs();
    private SendPacket packetTemp;
    //当前发送的packet大小以及进度
    private int total;
    private int position;

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
    }

    @Override
    public void send(SendPacket packet) {
        queue.offer(packet);//将数据放进队列中
        if (isSending.compareAndSet(false, true)) {
            sendNextPacket();
        }
    }


    @Override
    public void cancel(SendPacket packet) {

    }

    /**
     * 从队列中取数据
     *
     * @return
     */
    private SendPacket takePacket() {
        SendPacket packet = queue.poll();
        if (packet != null && packet.isCanceled()) {
            //已经取消不用发送
            return takePacket();
        }
        return packet;
    }

    private void sendNextPacket() {
        SendPacket temp = packetTemp;
        if (temp != null) {
            CloseUtil.close(temp);
        }
        //System.out.println("sendNextPacket");
        SendPacket packet = packetTemp = takePacket();
        if (packet == null) {
            //队列为空，取消发送状态
            isSending.set(false);
            return;
        }

        total = packet.length();
        position = 0;

        sendCurrentPacket();
    }

    private void sendCurrentPacket() {
        ioArgs args = ioArgs;

        args.startWriting();

        if (position >= total) {
            sendNextPacket();
            return;
        } else if (position == 0) {
            //首包，需要携带长度信息
            args.writeLength(total);
        }

        byte[] bytes = packetTemp.bytes();
        //System.out.println("把bytes的数据写入到IoArgs中");
        //把bytes的数据写入到IoArgs中
        int count = args.readFrom(bytes, position);
        position += count;

        //完成封装
        args.finishWriting();
        //进行消息发送

        System.out.println("进行消息发送"+position);
        try {
            sender.sendAsync(args, ioArgsEventListener);
        } catch (IOException e) {
            closeAndNotify();
        }

    }

    private void closeAndNotify() {
        CloseUtil.close(this);
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            isSending.set(false);
            SendPacket packet = packetTemp;
            if (packet != null) {
                packetTemp = null;
                CloseUtil.close(packet);
            }
        }
    }

    private ioArgs.IoArgsEventListener ioArgsEventListener = new ioArgs.IoArgsEventListener() {
        @Override
        public void onStarted(ioArgs args) {

        }

        @Override
        public void onCompleted(ioArgs args) {
            //继续发送当前包，因为可能一个包没发完
            sendCurrentPacket();
        }
    };


}
