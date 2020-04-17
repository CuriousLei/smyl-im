package cn.buptleida.niohdl.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * 公共的数据封装
 * 提供了类型以及基本的长度的定义
 */
public class Packet implements Closeable {
    protected byte type;
    protected int length;

    public byte type(){
        return type;
    }

    public int length(){
        return length;
    }

    @Override
    public void close() throws IOException {

    }
}
