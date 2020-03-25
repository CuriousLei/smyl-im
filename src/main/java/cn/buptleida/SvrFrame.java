package cn.buptleida;

import cn.buptleida.iohdl.ClientHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class SvrFrame implements ClientHandler.ClientHandlerCallBack {
    private final String svrIpAddr;
    private final int svrPort;
    private ServerSocket serverSocket;
    private ClientListen clientListenHandler;
    private final ArrayList<ClientHandler> clientHandlerList;
    private final ExecutorService routeThreadExecutor;//用于消息转发的单例线程池

    SvrFrame(String svrIpAddr, int svrPort) {
        this.svrIpAddr = svrIpAddr;
        this.svrPort = svrPort;
        this.clientHandlerList = new ArrayList<>();
        this.routeThreadExecutor = Executors.newSingleThreadExecutor();
    }


    void InitSocket() throws IOException {

        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(InetAddress.getByName(svrIpAddr), svrPort));

        // 是否复用未完全关闭的地址端口
        serverSocket.setReuseAddress(true);

        // 等效Socket#setReceiveBufferSize
        serverSocket.setReceiveBufferSize(64 * 1024 * 1024);

        // 设置serverSocket#accept超时时间，不设置即永久等待
        // serverSocket.setSoTimeout(2000);

        // 设置性能参数：短链接，延迟，带宽的相对重要性
        serverSocket.setPerformancePreferences(1, 1, 1);
    }


    void stop() throws IOException {
        for (ClientHandler client:clientHandlerList) {
            client.socketClose();
        }
        clientHandlerList.clear();
        clientListenHandler.exit();
        routeThreadExecutor.shutdown();
    }
    void start() {

        //创建线程监听客户端连接
        clientListenHandler = new ClientListen(serverSocket);
        clientListenHandler.start();
    }

    @Override
    public synchronized void ExitNotify(ClientHandler clientHandler) {

        for (ClientHandler client:clientHandlerList) {
            if(clientHandler == client){
                clientHandlerList.remove(clientHandler);
                break;
            }
        }
    }

    @Override
    public void NewMsgCallBack(ClientHandler srcClient, String msg) {
        routeThreadExecutor.execute(() -> {
            synchronized (SvrFrame.this) {
                //将用户发来的消息转发给其它用户
                for (ClientHandler destClient:clientHandlerList) {
                    if(srcClient == destClient){
                        continue;
                    }
                    destClient.write(srcClient.getUid()+" : "+msg);
                }
            }
        });
    }

    /**
     * 监听客户端连接请求的线程
     */
    class ClientListen extends Thread {
        private final ServerSocket serverSocket;
        private Boolean done = false;

        ClientListen(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        @Override
        public void run() {
            super.run();
            try {
                do {
                    Socket client;
                    try {
                        client = serverSocket.accept();
                    } catch (Exception e) {
                        continue;//某一个客户端连接失败，要保证其它客户端能正常连接
                    }
                    String uuid = UUID.randomUUID().toString();//为客户端生成唯一标识
                    System.out.println("已接受连接client：" + uuid + " /Addr:" + client.getInetAddress() + " /Port:" + client.getPort());

                    ClientHandler clientHandle = new ClientHandler(client, SvrFrame.this, uuid);
                    clientHandle.read();
                    clientHandlerList.add(clientHandle);
                } while (!done);
            } catch (Exception e) {
                if (!done) {
                    System.out.println("异常退出！");
                }
            }
        }

        void exit() throws IOException {
            done = true;
            serverSocket.close();
        }
    }
}
