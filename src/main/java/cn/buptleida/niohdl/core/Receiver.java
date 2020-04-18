package cn.buptleida.niohdl.core;

import java.io.Closeable;
import java.io.IOException;

public interface Receiver extends Closeable {
    void setReceiveListener(ioArgs.IoArgsEventListener listener);
    boolean receiveAsync(ioArgs args) throws IOException;
}
