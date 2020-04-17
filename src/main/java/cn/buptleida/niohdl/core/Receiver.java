package cn.buptleida.niohdl.core;

import java.io.Closeable;
import java.io.IOException;

public interface Receiver extends Closeable {
    boolean receiveAsync(ioArgs.IoArgsEventListener listener) throws IOException;
}
