package cn.buptleida.niohdl.core;

import java.io.Closeable;
import java.io.IOException;

public interface Sender extends Closeable {
    boolean sendAsync(ioArgs args, ioArgs.IoArgsEventListener listener) throws IOException;
}
