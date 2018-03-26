import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class InspectServer {
    private static final String url=
            "jdbc:mysql://localhost:3306/inspect" +//JDBC方式/MySQL数据库/本机/端口3306/数据库名称
                    "?useSSL=false&useUnicode=true&characterEncoding=utf8";//SSL关闭/使用Unicode编码/编码方式utf-8
    private static final String username="root";
    private static final String password="root";
    public static void main(String[] args) {
        //创建线程池等待任务队列，数量10
        BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(10);
        //创建线程池，基本线程5个，最大20个，存活时间2分钟
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                5, 20, 2, TimeUnit.MINUTES, blockingQueue);

        try {
            //反射加载驱动
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try (//连接数据库对象实例化
             Connection connection = DriverManager.getConnection(url, username, password);
             //获取statement数据库操作实例
             Statement statement = connection.createStatement();
             //serverSocket实例绑定60001端口
             ServerSocket serverSocket = new ServerSocket(60001)) {

            while (statement != null) {
                //启动侦听并进入阻塞状态
                Socket socket = serverSocket.accept();
                //启动线程
                threadPoolExecutor.execute(new InspectServerThread(statement, socket));
            }

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }
}
