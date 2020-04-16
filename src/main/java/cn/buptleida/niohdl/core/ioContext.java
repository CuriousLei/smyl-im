package cn.buptleida.niohdl.core;

import cn.buptleida.niohdl.impl.ioSelectorProvider;

public class ioContext {
    private static ioSelectorProvider ioSelector;

    public static ioSelectorProvider getIoSelector() {
        return ioSelector;
    }

    public static void setIoSelector(ioSelectorProvider ioSelector) {
        ioContext.ioSelector = ioSelector;
    }

    public static void close(){
        ioSelector.close();
    }
}
