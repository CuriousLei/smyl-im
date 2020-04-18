package cn.buptleida.niohdl.impl.async;

import cn.buptleida.niohdl.box.StringReceivePacket;
import cn.buptleida.niohdl.core.ReceiveDispatcher;
import cn.buptleida.niohdl.core.ReceivePacket;
import cn.buptleida.niohdl.core.Receiver;
import cn.buptleida.niohdl.core.ioArgs;
import cn.buptleida.utils.CloseUtil;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncReceiveDispatcher implements ReceiveDispatcher {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private final Receiver receiver;
    private final ReceivePacketCallback callback;

    private ioArgs args = new ioArgs();
    private ReceivePacket packetTemp;
    private byte[] buffer;
    private int total;
    private int position;

    public AsyncReceiveDispatcher(Receiver receiver, ReceivePacketCallback callback) {
        this.receiver = receiver;
        this.receiver.setReceiveListener(ioArgsEventListener);
        this.callback = callback;
    }

    @Override
    public void start() {
        registerReceive();
    }

    private void registerReceive() {

        try {
            receiver.receiveAsync(args);
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    private void closeAndNotify() {
        CloseUtil.close(this);
    }

    @Override
    public void stop() {

    }

    @Override
    public void close() throws IOException {
        if(isClosed.compareAndSet(false,true)){
            ReceivePacket packet = packetTemp;
            if(packet!=null){
                packetTemp = null;
                CloseUtil.close(packet);
            }
        }
    }

    private ioArgs.IoArgsEventListener ioArgsEventListener = new ioArgs.IoArgsEventListener() {
        @Override
        public void onStarted(ioArgs args) {
            int receiveSize;
            if (packetTemp == null) {
                receiveSize = 4;
            } else {
                receiveSize = Math.min(total - position, args.capacity());
            }
            //设置接受数据大小
            args.setLimit(receiveSize);
        }

        @Override
        public void onCompleted(ioArgs args) {
            assemblePacket(args);
            //继续接受下一条数据
            registerReceive();
        }
    };

    /**
     * 解析数据到packet
     *
     * @param args
     */
    private void assemblePacket(ioArgs args) {
        if (packetTemp == null) {
            int length = args.readLength();
            packetTemp = new StringReceivePacket(length);
            buffer = new byte[length];
            total = length;
            position = 0;
        }
        //将args中的数据写进buffer中
        int count = args.writeTo(buffer,0);
        //System.out.println("count="+count);
        if(count>0){
            packetTemp.save(buffer,count);
            position+=count;
            
            if(position == total){
                completePacket();
                packetTemp = null;
            }
        }
        
    }

    private void completePacket() {
        ReceivePacket packet = this.packetTemp;
        CloseUtil.close(packet);
        callback.onReceivePacketCompleted(packet);
    }

}
