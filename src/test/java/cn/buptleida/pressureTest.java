package cn.buptleida;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class pressureTest {
    private static boolean done;

    public static void main(String[] args) {

        List<Connector> ClientList = new ArrayList<>();
        for (int i = 0; i < 1000; ++i) {

            try {
                Connector client = new Connector();
                if (client == null) {
                    System.out.println("连接异常");
                    continue;
                }
                ClientList.add(client);
            } catch (IOException e) {
                System.out.println("创建客户端失败");
            }

            //定个延时
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        systemRead();

        Runnable runnable = () -> {
            while (!done) {
                for (Connector client : ClientList) {
                    try {
                        client.write("Hello~");
                    } catch (IOException e) {
                        System.out.println("发送失败");
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();

        systemRead();
        //结束
        done=true;

        //主线程等待runnable线程完成
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (Connector client : ClientList) {
            try {
                client.stop();
            } catch (IOException e) {
                System.out.println("关闭失败");
            }
        }
        ClientList.clear();
    }

    static void systemRead() {
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
