package cn.buptleida.clihdl;

import cn.buptleida.niohdl.API;
import cn.buptleida.niohdl.core.Connector;
import cn.buptleida.niohdl.core.ioArgs;
import cn.buptleida.niohdl.core.ioContext;
import cn.buptleida.utils.CloseUtil;

import java.awt.image.DataBufferByte;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.channels.SelectionKey.OP_READ;


public class ClientHandler {
    private final SocketChannel socketChannel;
    private final ReadHandler readHandler;//阻塞IO模式的读取线程操作
    private final WriteHandle writeHandler;//阻塞IO模式的输出线程操作
    //private final Removable removable;
    private final String uid;
    private final ClientHandlerCallBack clientHandlerCallBack;

    public ClientHandler(SocketChannel socketChannel, ClientHandlerCallBack clientHandlerCallBack, String uid) throws IOException {
        this.socketChannel = socketChannel;
        this.readHandler = new ReadHandler(socketChannel.socket().getInputStream());
        this.writeHandler = new WriteHandle(socketChannel.socket().getOutputStream(), socketChannel);


        Connector connector = new Connector(){
            /**
             * 收到客户端消息，从niohdl里面回调，args里面包含buffer字节数组存储数据
             */
            @Override
            protected void onReceiveFromCore(ioArgs args) {
                super.onReceiveFromCore(args);
                clientHandlerCallBack.NewMsgCallBack(ClientHandler.this, args);
                ByteBuffer argsBuffer = args.getBuffer();
                System.out.println(new String(argsBuffer.array(), 0, argsBuffer.position() - 1));
                //回调完再次注册，读取下一条数据
                //必须要这样
                //ioContext.getIoSelector().registerInput(socketChannel, ClientHandler.this);
            }

            /**
             * runnable处理里面异常退出的回调
             */
            @Override
            public void onChannelClosed() {
                super.onChannelClosed();
                exitSelf();
            }
        };

        connector.setup(socketChannel);


        //Selector writeSelector = Selector.open();
        //socketChannel.register(writeSelector, OP_READ);
        this.clientHandlerCallBack = clientHandlerCallBack;
        this.uid = uid;

        //开始BIO模式的读取线程
        //read();
    }

    public interface ClientHandlerCallBack {

        //用户退出的回调
        void ExitNotify(ClientHandler clientHandler);

        //用户传来新消息的回调,BIO模式
        void NewMsgCallBack(ClientHandler clientHandler, String msg);

        //用户传来新消息的回调,NIO模式
        void NewMsgCallBack(ClientHandler clientHandler, ioArgs args);
    }

    public void read() {
        readHandler.start();
    }

    public void write(String msg) {

        writeHandler.write(msg);
    }

    public void write(ioArgs args) {
        //System.out.println(msg);
        writeHandler.write(args);
    }

    /**
     * 把输入输出流和套接字都关闭
     */
    public void socketClose() {
        readHandler.exit();
        writeHandler.exit();
        CloseUtil.close(socketChannel);
    }

    /**
     * clientHandler退出
     * 关闭套接字通道，把自身从对象列表中清除掉
     */
    private void exitSelf() {
        socketClose();
        clientHandlerCallBack.ExitNotify(this);
    }



    /**
     * 阻塞式IO
     * 输入流操作线程
     */
    class ReadHandler extends Thread {
        private final BufferedReader socketInput;
        private Boolean flag = true;

        ReadHandler(InputStream inputStream) {
            socketInput = new BufferedReader(new InputStreamReader(inputStream));
        }

        @Override
        public void run() {
            super.run();

            try {
                do {

                    String str = socketInput.readLine();
                    //不知道为什么，客户端关闭时，这里直接报异常，获取不到null
                    if (str == null) {
                        System.out.println("已无法读取客户端" + ClientHandler.this.uid + "的数据！");
                        throw new Exception();
                    }
                    ClientHandler.this.clientHandlerCallBack.NewMsgCallBack(ClientHandler.this, str);
                    System.out.println(uid + " -->> server : " + str);
                } while (flag);
            } catch (Exception e) {
                if (flag) {
                    System.out.println("读取客户端过程中异常退出");
                    ClientHandler.this.exitSelf();
                }
            }
        }

        void exit() {
            flag = false;
            CloseUtil.close(socketInput);
        }
    }

    /**
     * 阻塞式IO
     * 输出流操作线程，使用单例线程池，可以自动等待任务并处理，无需人工添加阻塞操作
     */
    class WriteHandle {
        private boolean done = false;
        private final PrintStream printStream;
        private final SocketChannel channel;
        private final ExecutorService executorService;

        WriteHandle(OutputStream outputStream, SocketChannel channel) {
            this.printStream = new PrintStream(outputStream);
            this.channel = channel;
            this.executorService = Executors.newSingleThreadExecutor();
        }

        private void write(String msg) {
            executorService.execute(new WriteRunnable(msg, printStream));
        }

        private void write(ioArgs args) {
            executorService.execute(new WriteRunnable(args));
        }

        void exit() {
            this.done = true;
            CloseUtil.close(printStream);
            executorService.shutdown();
        }

        class WriteRunnable implements Runnable {
            private final String msg;
            private PrintStream printStream;
            private ByteBuffer byteBuffer;//输出缓冲区

            WriteRunnable(String msg, PrintStream printStream) {
                this.msg = msg;
                this.printStream = printStream;
            }

            WriteRunnable(ioArgs args) {
                StringBuilder stringBuilder = new StringBuilder(args.getSrcUid());
                stringBuilder.append(" : ");
                ByteBuffer argsBuffer = args.getBuffer();
                stringBuilder.append(new String(argsBuffer.array(), 0, argsBuffer.position() - 1));
                this.msg = stringBuilder.toString();
                this.byteBuffer = ByteBuffer.allocate(256);
            }

            @Override
            public void run() {
                if (WriteHandle.this.done) {
                    return;
                }

                byteBuffer.clear();
                byteBuffer.put(msg.getBytes());
                byteBuffer.flip();

                System.out.println(msg.length()+" "+byteBuffer.position()+" "+byteBuffer.limit());
                while (!done && byteBuffer.hasRemaining()) {
                    try {
                        System.out.println(msg);
                        int len = WriteHandle.this.channel.write(byteBuffer);
                        //在NIO中并不一定能发送，若不能发送，则返回0
                        if (len < 0) {
                            System.out.println("客户端handler不可发送数据");
                            ClientHandler.this.exitSelf();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("打印输出异常！");
                        done = true;
                    }
                }


            }
        }
    }

    public String getUid() {
        return uid;
    }
}
