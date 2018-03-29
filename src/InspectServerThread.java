import javax.net.ssl.SSLSocket;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class InspectServerThread implements Runnable {
    //创建对象
    private Statement statement;
    private SSLSocket sslSocket;

    //构造函数完成初始化类
    InspectServerThread(Statement statement, SSLSocket sslSocket) {
        this.statement = statement;
        this.sslSocket = sslSocket;
    }

    @Override
    public void run() {
        String[] splitMessage;  //存储拆分消息字符串数组
        String message; //消息字符串
        try (//创建输入流对象
             InputStream inputStream = sslSocket.getInputStream();
             //读取输入流
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
             //缓存输入流
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
             OutputStream outputStream = sslSocket.getOutputStream();
             PrintWriter printWriter = new PrintWriter(outputStream)) {
            //注：一次读取一行，发送时必须要换行
            message = bufferedReader.readLine();
            //调用split()方法以“#”拆分若干段
            splitMessage = message.split("#");
            switch (splitMessage[0]) {
                //请求码"0"，即登陆操作
                case "0": {
                    //对象实例化，完成SQL语句执行
                    ResultSet resultSet = statement.executeQuery(
                            "SELECT * FROM user_table WHERE username=\'" + splitMessage[1] + "\' AND password=\'" + splitMessage[2] + "\'");
                    //查询数据库返回结果，有结果则用户合法返回"1"或"2"，否则用户非法返回其他
                    if (resultSet.next()) {
                        //admin权限位为true时，返回管理员代码"0"，否则返回用户代码"1"
                        if (resultSet.getBoolean(3)) {
                            printWriter.println("0");
                        } else {
                            printWriter.println("1");
                        }
                    } else {
                        printWriter.println("2");
                    }
                    printWriter.flush();
                    break;
                }
                //请求码"1"，即信息支持操作
                case "1":

                    break;
                //请求码"2"，即其他操作
                case "2":

                    break;
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }
}
