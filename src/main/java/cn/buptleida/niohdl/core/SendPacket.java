package cn.buptleida.niohdl.core;

/**
 * 发送包的定义
 */
public abstract class SendPacket extends Packet {
    //标记是否已结束
    private boolean isCanceled;

    public abstract byte[] bytes();

    public boolean isCanceled() {
        return isCanceled;
    }
}
