package cn.buptleida.niohdl.box;

import cn.buptleida.niohdl.core.SendPacket;

import java.io.*;

public class FileSendPacket extends SendPacket {

    public FileSendPacket(File file){
        this.length = file.length();
    }
    @Override
    protected FileInputStream createStream() {
        return null;
    }
}
