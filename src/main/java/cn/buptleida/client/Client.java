package cn.buptleida.client;

import cn.buptleida.niohdl.core.ioContext;
import cn.buptleida.niohdl.impl.ioSelectorProvider;

import java.io.*;

public class Client {
    public static void main(String[] args)throws IOException {

        ioContext.setIoSelector(new ioSelectorProvider());

        String ServerIp = "127.0.0.1";
        int ServerPort = 8008;

        try {
            NIOConnector connector = NIOConnector.startWith(ServerIp,ServerPort);
            //connector.send("hello");
            write(connector);
        }catch (Exception e){
            System.out.println("连接失败，退出");
        }


        ioContext.close();
    }

    /**
     * 输出流方法
     */
    private static void write(NIOConnector connector) throws IOException {
        //构建键盘输入流
        InputStream in = System.in;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));


        for(;;){
            String str = bufferedReader.readLine();//从键盘输入获取内容
            connector.send(str);
            if(str.equalsIgnoreCase("exit")){
                break;
            }
        }
        System.out.println("输出流关闭");
    }
}
