package cn.buptleida.niohdl.core;

import cn.buptleida.niohdl.impl.SocketChannelAdapter;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Connector implements Closeable, SocketChannelAdapter.OnChannelStatusChangedListener {

    private SocketChannel channel;
    private Sender sender;//这两个都引用适配器
    private Receiver receiver;
    private ioArgs.IoArgsEventListener echoReceiveListener = new ioArgs.IoArgsEventListener() {
        @Override
        public void onStarted(ioArgs args) {

        }

        @Override
        public void onCompleted(ioArgs args) {
            onReceiveFromCore(args);
            readNextMessage();
        }
    };

    public void setup(SocketChannel channel) throws IOException {

        this.channel = channel;
        SocketChannelAdapter adapter = new SocketChannelAdapter(channel, ioContext.getIoSelector(), this);
        sender = adapter;
        receiver = adapter;

        readNextMessage();
    }

    private void readNextMessage() {

        if(receiver!=null){
            try {
                //将回调方法传给适配器，并执行注册
                receiver.receiveAsync(echoReceiveListener);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //这两个方法给ClientHandler中的匿名子类继承重写
    protected void onReceiveFromCore(ioArgs args) {
    }

    protected void onChannelClosed() {
    }


    //实现Closeable方法
    @Override
    public void close() throws IOException {

    }

    //实现SocketChannelAdapter.OnChannelStatusChangedListener中的方法
    @Override
    public void onChannelClosed(SocketChannel channel) {

    }
}
