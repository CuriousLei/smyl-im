package cn.buptleida;

import cn.buptleida.clihdl.ClientHandler;
import cn.buptleida.niohdl.core.ioArgs;
import cn.buptleida.niohdl.core.ioContext;
import cn.buptleida.niohdl.impl.ioSelectorProvider;
import cn.buptleida.utils.CloseUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class SvrFrame implements ClientHandler.ClientHandlerCallBack {
    private final String svrIpAddr;
    private final int svrPort;
    //private ServerSocket serverSocket;
    private Selector selector;
    private ClientListen clientListenHandler;
    private final ArrayList<ClientHandler> clientHandlerList;
    private final ExecutorService routeThreadExecutor;//用于消息转发的单例线程池

    SvrFrame(String svrIpAddr, int svrPort) {
        this.svrIpAddr = svrIpAddr;
        this.svrPort = svrPort;
        this.clientHandlerList = new ArrayList<>();
        this.routeThreadExecutor = Executors.newSingleThreadExecutor();
    }


    /**
     * 建立selector以及serverChannel，进行注册绑定，用于监听客户端连接请求
     * @throws IOException
     */
    void InitSocket() throws IOException {

        //serverSocket = new ServerSocket();
        //serverSocket.bind(new InetSocketAddress(InetAddress.getByName(svrIpAddr), svrPort));

        // 是否复用未完全关闭的地址端口
        //serverSocket.setReuseAddress(true);

        // 等效Socket#setReceiveBufferSize
        //serverSocket.setReceiveBufferSize(64 * 1024 * 1024);

        // 设置serverSocket#accept超时时间，不设置即永久等待
        // serverSocket.setSoTimeout(2000);

        // 设置性能参数：短链接，延迟，带宽的相对重要性
        //serverSocket.setPerformancePreferences(1, 1, 1);

        //下面是NIO方法
        //开启一个selector
        selector = Selector.open();
        //开启一个channel用于监听客户端连接请求
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        //设置为非阻塞
        serverSocketChannel.configureBlocking(false);
        //绑定本地ip端口
        serverSocketChannel.socket().bind(new InetSocketAddress(InetAddress.getByName(svrIpAddr), svrPort));
        //将channel注册到selector上
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);


    }

    /**
     * 建立read和write的selector，用于监视后续的客户端channel;
     * 同时建立输入输出的线程池
     * @throws IOException
     */
    void InitIOSelector() throws IOException {

        ioContext.setIoSelector(new ioSelectorProvider());
    }


    void stop() throws IOException {
        for (ClientHandler client : clientHandlerList) {
            client.socketClose();
        }
        clientHandlerList.clear();
        clientListenHandler.exit();
        routeThreadExecutor.shutdown();

        //关闭核心
        ioContext.close();
    }

    void start() {

        //创建线程监听客户端连接
        clientListenHandler = new ClientListen(selector);
        clientListenHandler.start();
    }

    @Override
    public synchronized void ExitNotify(ClientHandler clientHandler) {

        for (ClientHandler client : clientHandlerList) {
            if (clientHandler == client) {
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
                for (ClientHandler destClient : clientHandlerList) {
                    if (srcClient == destClient) {
                        continue;
                    }
                    System.out.println(destClient.getUid());
                    destClient.write(srcClient.getUid() + " : " + msg);
                }
            }
        });
    }
    @Override
    public void NewMsgCallBack(ClientHandler srcClient, ioArgs args) {
        routeThreadExecutor.execute(() -> {
            synchronized (SvrFrame.this) {
                args.setSrcUid(srcClient.getUid());
                //将用户发来的消息转发给其它用户
                for (ClientHandler destClient : clientHandlerList) {
                    if (srcClient == destClient) {
                        continue;
                    }
                    destClient.write(args);
                }
            }
        });
    }

    /**
     * 监听客户端连接请求的线程
     */
    class ClientListen extends Thread {
        private final Selector selector;
        private Boolean done = false;

        ClientListen(Selector selector) {
            this.selector = selector;
        }

        @Override
        public void run() {
            super.run();
            try {
                do {

                    //selector监听通道是否就绪，未就绪时select()返回0
                    if(selector.select()==0){
                        if(done){
                            break;
                        }
                        continue;
                    }
                    //通过迭代器来遍历序列的对象
                    Iterator<SelectionKey> iterator =selector.selectedKeys().iterator();
                    while(iterator.hasNext()){
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        if(key.isAcceptable()){
                            //获取用于监听连接的serverChannel
                            ServerSocketChannel serverChannel = (ServerSocketChannel)key.channel();
                            //通过accept获取socketChannel，对应于一个客户端
                            SocketChannel socketChannel = serverChannel.accept();

                            String uuid = UUID.randomUUID().toString();//为客户端生成唯一标识
                            System.out.println("已接受连接client：" + uuid
                                    + " /Addr:" + socketChannel.getRemoteAddress()
                                    + " /Port:" + socketChannel.socket().getPort());
                            //创建对象，用于处理客户端消息收发
                            ClientHandler clientHandle = new ClientHandler(socketChannel, SvrFrame.this, uuid);
                            //clientHandle.read();
                            //这里只有一个线程，貌似加锁没必要
                            synchronized (SvrFrame.this){
                                clientHandlerList.add(clientHandle);
                            }
                        }
                    }



                } while (!done);
            } catch (Exception e) {
                if (!done) {
                    System.out.println("异常退出！");
                }
            }
        }

        void exit(){
            done = true;
            CloseUtil.close(selector);
        }
    }
}
