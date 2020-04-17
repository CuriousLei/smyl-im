package cn.buptleida;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

class Connector {
    private final Socket clientSocket;
    private final PrintStream printStream;
    private final ReadHandle readHandle;

    Connector() throws IOException {
        this.clientSocket = createSocket();
        InitSocket(clientSocket);//初始化套接字
        //连接远程服务器
        clientSocket.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 8008), 3000);
        System.out.println("已连接server");

        //创建read线程
        this.readHandle = new ReadHandle(clientSocket.getInputStream());
        readHandle.start();

        //得到socket输出流并转化为打印流
        OutputStream outputStream = this.clientSocket.getOutputStream();
        this.printStream = new PrintStream(outputStream);
    }


    void stop() throws IOException {
        printStream.close();
        readHandle.exit();
        clientSocket.close();

    }
    private Socket createSocket() {
/*
        // 无代理模式，等效于空构造函数
        Socket socket = new Socket(Proxy.NO_PROXY);

        // 新建一份具有HTTP代理的套接字，传输数据将通过www.baidu.com:8080端口转发
        Proxy proxy = new Proxy(Proxy.Type.HTTP,
                new InetSocketAddress(Inet4Address.getByName("www.baidu.com"), 8800));
        socket = new Socket(proxy);

        // 新建一个套接字，并且直接链接到本地20000的服务器上
        socket = new Socket("localhost", PORT);

        // 新建一个套接字，并且直接链接到本地20000的服务器上
        socket = new Socket(Inet4Address.getLocalHost(), PORT);

        // 新建一个套接字，前面两个是远程服务器端，后面两个是自己
        socket = new Socket("localhost", PORT, Inet4Address.getLocalHost(), LOCAL_PORT);
        socket = new Socket(Inet4Address.getLocalHost(), PORT, Inet4Address.getLocalHost(), LOCAL_PORT);
        */
        Socket socket = new Socket();
        //socket.bind(new InetSocketAddress(InetAddress.getLocalHost(), LOCAL_PORT));
        return socket;
    }

    private void InitSocket(Socket socket) throws SocketException {
        // 设置读取超时时间为2秒，超过2秒未获得数据时readline报超时异常；不设置即进行永久等待
        //socket.setSoTimeout(2000);

        // 是否复用未完全关闭的Socket地址，对于指定bind操作后的套接字有效
        socket.setReuseAddress(true);

        // 是否开启Nagle算法
        socket.setTcpNoDelay(true);

        // 是否需要在长时无数据响应时发送确认数据（类似心跳包），时间大约为2小时
        socket.setKeepAlive(true);

        // 对于close关闭操作行为进行怎样的处理；默认为false，0
        // false、0：默认情况，关闭时立即返回，底层系统接管输出流，将缓冲区内的数据发送完成
        // true、0：关闭时立即返回，缓冲区数据抛弃，直接发送RST结束命令到对方，并无需经过2MSL等待
        // true、200：关闭时最长阻塞200毫秒，随后按第二情况处理
        socket.setSoLinger(true, 20);

        // 是否让紧急数据内敛，默认false；紧急数据通过 socket.sendUrgentData(1);发送
        socket.setOOBInline(true);

        // 设置接收发送缓冲器大小
        socket.setReceiveBufferSize(64 * 1024 * 1024);
        socket.setSendBufferSize(64 * 1024 * 1024);

        // 设置性能参数：短链接，延迟，带宽的相对重要性
        socket.setPerformancePreferences(1, 1, 1);
    }

    /**
     * 输出流方法
     */
    void write(String msg) throws IOException {

        printStream.println(msg);//通过打印流输出

    }


    /**
     * 输入流线程
     */
    static class ReadHandle extends Thread {
        private final InputStream inputStream;
        private Boolean done = false;

        ReadHandle(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            super.run();
            try {
                //获取输入流
                BufferedReader socketInput = new BufferedReader(new InputStreamReader(inputStream));
                do {
                    String str;
                    str = socketInput.readLine();
                    if (str==null) {
                        break;
                    }
                    System.out.println(str);
                } while (!done);
            } catch (Exception e) {
                if (!done) {
                    System.out.println("异常断开，或者输入异常");
                }
            }
        }

        void exit() {
            done = true;
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                System.out.println("输入流关闭");
            }
        }
    }
}
