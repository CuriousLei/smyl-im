package cn.buptleida.niohdl.impl;


import cn.buptleida.niohdl.core.Receiver;
import cn.buptleida.niohdl.core.Sender;
import cn.buptleida.niohdl.core.ioArgs;
import cn.buptleida.niohdl.core.ioProvider;
import cn.buptleida.utils.CloseUtil;


import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketChannelAdapter implements Sender, Receiver, Cloneable {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final SocketChannel channel;
    private final ioProvider provider;
    private final OnChannelStatusChangedListener listener;//指向Connector对象,只有onChannelClosed方法

    private ioArgs.IoArgsEventListener receiveIoEventListener;//2个都是指向Connector中的echoReceiveListener对象，可借此向Connector回调消息
    private ioArgs.IoArgsEventListener sendIoEventListener;

    public SocketChannelAdapter(SocketChannel channel, ioProvider provider,
                                OnChannelStatusChangedListener listener) throws IOException {
        this.channel = channel;
        this.provider = provider;
        this.listener = listener;

        channel.configureBlocking(false);
    }


    @Override
    public boolean receiveAsync(ioArgs.IoArgsEventListener listener) throws IOException {
        if (isClosed.get()) {
            throw new IOException("Current channel is closed!");
        }

        receiveIoEventListener = listener;

        return provider.registerInput(channel, inputHandler);
    }

    @Override
    public boolean sendAsync(ioArgs args, ioArgs.IoArgsEventListener listener) throws IOException {
        if (isClosed.get()) {
            throw new IOException("Current channel is closed!");
        }

        sendIoEventListener = listener;
        // 当前发送的数据附加到回调中
        outputHandler.setAttach(args);
        return provider.registerOutput(channel, outputHandler);
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            // 解除注册回调
            provider.unRegisterInput(channel);
            provider.unRegisterOutput(channel);
            // 关闭
            CloseUtil.close(channel);
            // 回调当前Channel已关闭
            listener.onChannelClosed(channel);
        }
    }

    private final ioProvider.InputHandler inputHandler = new ioProvider.InputHandler() {
        @Override
        protected void handle() {
            if (isClosed.get()) {
                return;
            }

            ioArgs args = new ioArgs();
            ioArgs.IoArgsEventListener listener = SocketChannelAdapter.this.receiveIoEventListener;

            System.out.println("test");
            if (listener != null) {
                //开始处理，向Connector中的echoReceiveListener回调消息
                listener.onStarted(args);
            }

            try {
                // 具体的读取操作
                if (args.read(channel) > 0 && listener != null) {
                    // 读取完成回调
                    listener.onCompleted(args);
                } else {
                    throw new IOException("Cannot read any data!");
                }
            } catch (IOException ignored) {
                CloseUtil.close(SocketChannelAdapter.this);
            }


        }
    };


    private final ioProvider.OutputHandler outputHandler = new ioProvider.OutputHandler() {
        @Override
        protected void handle(Object attach) {
            if (isClosed.get()) {
                return;
            }

            sendIoEventListener.onCompleted(null);
        }
    };


    public interface OnChannelStatusChangedListener {
        void onChannelClosed(SocketChannel channel);
    }
}
