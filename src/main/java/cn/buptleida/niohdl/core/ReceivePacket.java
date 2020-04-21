package cn.buptleida.niohdl.core;

import java.io.OutputStream;

/**
 * 接收包的定义
 */
public abstract class ReceivePacket<T extends OutputStream> extends Packet<T> {
    //public abstract void save(byte[] bytes, int count);

    //public abstract OutputStream open();
}
