package cn.buptleida;

import java.io.IOException;


public class Client {
    public static void main(String[] args)throws IOException {
        String ServerIp = args[0];
        int ServerPort = Integer.parseInt(args[1]);

        try {
            Connector connector =new Connector();
            //connector.connect(ServerIp, ServerPort);
        }catch (Exception e){
            System.out.println("连接失败，退出");
        }
    }
}
