package cn.buptleida.niohdl.box;

import cn.buptleida.niohdl.core.ReceivePacket;

public class StringReceivePacket extends ReceivePacket {
    private byte[] buffer;
    private int position;

    public StringReceivePacket(int len) {
        this.buffer = new byte[len];
        length = len;
    }

    @Override
    public void save(byte[] bytes, int count) {
        //position是目标数组的起始长度，count是源数组的要copy的长度
        System.arraycopy(bytes,0,buffer,position,count);
        position+=count;
    }

    public String string(){
        return new String(buffer);
    }
}
