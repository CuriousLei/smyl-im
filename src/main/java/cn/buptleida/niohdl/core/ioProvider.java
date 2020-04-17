package cn.buptleida.niohdl.core;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public interface ioProvider {
    boolean registerInput(SocketChannel channel, InputHandler inputHandler);

    boolean registerOutput(SocketChannel channel, OutputHandler outputHandler);

    void unRegisterInput(SocketChannel channel);

    void unRegisterOutput(SocketChannel channel);

    abstract class InputHandler implements Runnable {

        @Override
        public final void run() {
            handle();
        }

        protected abstract void handle();
    }

    abstract class OutputHandler implements Runnable {
        Object args;
        public final void setAttach(Object attach) {
            this.args = attach;
        }

        @Override
        public final void run() {
            handle(args);
        }

        protected abstract void handle(Object args);
    }

    interface IOCallback {
        void onInput(ioArgs args);

        void onOutput();

        void onChannelClosed();
    }
}
