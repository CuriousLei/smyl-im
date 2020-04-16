package cn.buptleida.niohdl;

import cn.buptleida.niohdl.core.ioArgs;
import cn.buptleida.niohdl.core.ioContext;
import cn.buptleida.niohdl.core.ioProvider;

import java.nio.channels.SocketChannel;

public abstract class API implements ioProvider.IOCallback {
    private SocketChannel socketChannel;

    @Override
    public void onInput(ioArgs args) {
        onReceiveFromCore(args);
    }

    protected void onReceiveFromCore(ioArgs args){
    }

    @Override
    public void onOutput() {

    }

    @Override
    public void onChannelClosed() {
        ioContext.getIoSelector().unRegisterInput(socketChannel);
        ioContext.getIoSelector().unRegisterOutput(socketChannel);
    }
}
