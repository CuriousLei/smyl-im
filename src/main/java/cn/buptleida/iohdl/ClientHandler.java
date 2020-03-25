package cn.buptleida.iohdl;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ClientHandler {
    private final Socket client;
    private final ReadHandler readHandler;
    private final WriteHandle writeHandler;
    //private final Removable removable;
    private final String uid;
    private final ClientHandlerCallBack clientHandlerCallBack;

    public ClientHandler(Socket socket, ClientHandlerCallBack clientHandlerCallBack, String uid) throws IOException {
        this.client = socket;
        this.readHandler = new ReadHandler(socket.getInputStream());
        this.writeHandler = new WriteHandle(socket.getOutputStream());
        this.clientHandlerCallBack = clientHandlerCallBack;
        this.uid = uid;
    }
    public interface ClientHandlerCallBack{

        //用户退出的回调
        void ExitNotify(ClientHandler clientHandler);

        //用户传来新消息的回调
        void NewMsgCallBack(ClientHandler clientHandler, String msg);
    }

    public void read() {
        readHandler.start();
    }

    public void write(String msg) {
        System.out.println(msg);
        writeHandler.write(msg);
    }

    /**
     * 把输入输出流和套接字都关闭
     */
    public void socketClose(){
        try {
            readHandler.exit();
            writeHandler.exit();
            client.close();
        } catch (IOException e) {
            System.out.println("客户端："+uid+" 套接字关闭过程出现异常");
        }finally {
            System.out.println("客户端："+uid+" 套接字连接已关闭");
        }
    }
    /**
     * 把自身从对象列表中清除掉
     */
    private void removeSelf() {
        //removable.removeClientHandle(this);
        clientHandlerCallBack.ExitNotify(this);
    }


    /**
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
                    if (str==null) {
                        System.out.println("已无法读取客户端"+ClientHandler.this.uid+"的数据！");
                        throw new Exception();
                    }
                    ClientHandler.this.clientHandlerCallBack.NewMsgCallBack(ClientHandler.this, str);
                    System.out.println(uid + " -->> server : " + str);
                } while (flag);
            } catch (Exception e) {
                if (flag) {
                    System.out.println("读取客户端过程中异常退出");
                    ClientHandler.this.removeSelf();
                    ClientHandler.this.socketClose();
                }
            }
        }

        void exit() throws IOException {
            flag = false;
            socketInput.close();
        }
    }

    /**
     * 输出流操作线程，使用单例线程池，可以自动等待任务并处理，无需人工添加阻塞操作
     */
    class WriteHandle {
        private final PrintStream printStream;
        private final ExecutorService executorService;

        WriteHandle(OutputStream outputStream) {
            this.printStream = new PrintStream(outputStream);
            this.executorService = Executors.newSingleThreadExecutor();
        }
        private void write(String msg){
            executorService.execute(new WriteRunnable(msg,printStream));
        }
        void exit(){
            printStream.close();
            executorService.shutdown();
        }
        class WriteRunnable implements Runnable{
            private final String msg;
            private final PrintStream printStream;

            WriteRunnable(String msg, PrintStream printStream) {
                this.msg = msg;
                this.printStream = printStream;
            }

            @Override
            public void run() {
                try {
                    printStream.println(msg);
                } catch (Exception e) {
                    System.out.println("打印输出异常！");
                }

            }
        }
    }

    public String getUid() {
        return uid;
    }
}
