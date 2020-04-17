package cn.buptleida.niohdl.impl.async;

import cn.buptleida.niohdl.core.SendDispatcher;
import cn.buptleida.niohdl.core.SendPacket;
import cn.buptleida.niohdl.core.ioArgs;
import cn.buptleida.utils.CloseUtil;
import com.sun.corba.se.impl.protocol.giopmsgheaders.IORAddressingInfo;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncSendDispatcher implements SendDispatcher {
    //private final Sender sender;
    private final Queue<SendPacket> queue;
    private AtomicBoolean isSending = new AtomicBoolean();

    public AsyncSendDispatcher(Queue<SendPacket> queue) {
        this.queue = queue;
    }

    private ioArgs ioArgs = new ioArgs();
    private SendPacket packetTemp;
    //当前发送的packet大小以及进度
    private int total;
    private int position;

    @Override
    public void send(SendPacket packet) {
        queue.offer(packet);
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
        if(temp!=null){
            CloseUtil.close(temp);
        }
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
    private void sendCurrentPacket(){
        ioArgs args = ioArgs;

        args.startWriting();

        if(position>=total){
            sendNextPacket();
            return;
        }else if(position==0){
            //首包，需要携带长度信息
            args.writeLength(total);
        }

        byte[] bytes = packetTemp.bytes();
        //把bytes的数据写入到IoArgs中
        int count = args.readFrom(bytes,position);
        position +=count;

        //完成封装
        args.finishWriting();
        //进行消息发送

    }
}
