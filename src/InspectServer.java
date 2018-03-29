import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static javax.net.ssl.SSLContext.*;

public class InspectServer {
    private static final String url =
            "jdbc:mysql://localhost:3306/inspect" +//JDBC方式/MySQL数据库/本机/端口3306/数据库名称
                    "?useSSL=false&useUnicode=true&characterEncoding=utf8";//SSL关闭/使用Unicode编码/编码方式utf-8
    private static final String username = "root";
    private static final String password = "root";

    public static void main(String[] args) throws IOException {
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

        SSLSocket sslSocket = null;
        SSLServerSocket sslServerSocket = null;
        try (//连接数据库对象实例化
             Connection connection = DriverManager.getConnection(url, username, password);
             //获取statement数据库操作实例
             Statement statement = connection.createStatement()
        ) {
            //实例化SSL上下文对象，使用TLS协议(SSLv1-v3已经不安全被弃用，现阶段使用TLSv1.0-v1.2，API 19及以下不支持TLSv1.1和v1.2)
            SSLContext sslContext = getInstance("TLS");
            //设置秘钥库管理器，Sun公司遵守X509标准的SunX509
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
//            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            //装载服务器秘钥库，PKCS12对密钥对和证书管理，安全级别较高
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream("src/server.p12"), "server".toCharArray());
//            //装载客户端信任的秘钥库
//            KeyStore trustKeyStore = KeyStore.getInstance("PKCS12");
//            trustKeyStore.load(new FileInputStream("src/client_trust.p12"), "client".toCharArray());
            //初始化秘钥管理器，密码为server
            keyManagerFactory.init(keyStore, "server".toCharArray());
//            trustManagerFactory.init(trustKeyStore);
            //初始化SSL上下文对象，信任库管理器为空（双向验证时使用），指定安全的随机数作为加密数
            sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());

            //serverSocket实例绑定60001端口
            sslServerSocket = (SSLServerSocket) sslContext.getServerSocketFactory().createServerSocket(60001);
//            //启用双向验证
//            sslServerSocket.setNeedClientAuth(true);
            while (statement != null) {
                //启动侦听并进入阻塞状态
                sslSocket = (SSLSocket) sslServerSocket.accept();
                //启动线程
                threadPoolExecutor.execute(new InspectServerThread(statement, sslSocket));
            }
        } catch (SQLException | NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException | UnrecoverableKeyException | KeyManagementException e) {
            e.printStackTrace();
        } finally {
            if (sslServerSocket != null) {
                sslServerSocket.close();
            }
            if (sslSocket != null) {
                sslSocket.close();
            }
        }
    }
}
