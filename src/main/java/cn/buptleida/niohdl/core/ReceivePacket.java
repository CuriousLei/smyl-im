package cn.buptleida.niohdl.core;

/**
 * 接收包的定义
 */
public abstract class ReceivePacket extends Packet {
    public abstract void save(byte[] bytes, int count);
}
