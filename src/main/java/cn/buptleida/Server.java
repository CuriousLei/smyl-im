package cn.buptleida;

import java.io.IOException;

public class Server {
    public static void main(String[] args){

        String ConfigIp;
        int ConfigPort;

        ConfigIp = args[0];
        ConfigPort = Integer.parseInt(args[1]);
        System.out.println(ConfigIp+":"+ConfigPort);

        SvrFrame svrFrame = new SvrFrame(ConfigIp, ConfigPort);


        try {
            svrFrame.InitSocket();
        } catch (IOException e) {
            System.out.println("xxxxxxxxxxxxxxxxxxx Init SERVER SOCKET FAILED xxxxxxxxxxxxxxxxxxxxx");
        }

        svrFrame.start();
        System.out.println("xxxxxxxxxxxxxxxxxxx CHAT SERVER is running xxxxxxxxxxxxxxxxxxxxx");

    }
}
