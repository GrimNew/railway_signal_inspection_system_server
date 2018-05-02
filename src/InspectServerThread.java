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
        String[] splitMessage; //存储拆分消息字符串数组
        String message; //消息字符串
        String T_SQL;
        String username;
        try (//创建输入流对象
             InputStream inputStream = sslSocket.getInputStream();
             //读取输入流
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
             //缓存输入流
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
             OutputStream outputStream = sslSocket.getOutputStream();
             PrintWriter printWriter = new PrintWriter(outputStream)) {

            //循环响应登录请求
            while ((message = bufferedReader.readLine()) != null) {
                //调用split()方法以“#”拆分若干段
                splitMessage = message.split("#");
                //请求码"0"，登陆操作,识别不出请求码则跳出读取并关闭资源
                if (splitMessage[0].equals("0")) {
                    //对象实例化，完成SQL语句执行
                    ResultSet resultSet = statement.executeQuery(
                            "SELECT * FROM user_table WHERE username=\'" + splitMessage[1] + "\' AND password=\'" + splitMessage[2] + "\'");
                    //查询数据库返回结果，有结果则用户合法返回"00"或"01"，否则用户非法返回"02"
                    if (resultSet.next()) {
                        //admin权限位为true时，返回管理员代码"00"，否则返回用户代码"01"
                        if (resultSet.getBoolean(3)) {
                            printWriter.println("00");
                        } else {
                            printWriter.println("01");
                        }
                        printWriter.flush();

                        //记住当前登录用户
                        username = splitMessage[1];

                        //循环响应更新请求
                        while ((message = bufferedReader.readLine()) != null) {
                            splitMessage = message.split("#");
                            //请求码"1"，更新设备数据
                            if (splitMessage[0].equals("1")) {
                                T_SQL = "INSERT INTO data_table(device_id,username,status) VALUES(\'"
                                        + splitMessage[1] + "\',\'" + username + "\',\'" + splitMessage[2] + "\')";
                                //执行T-SQL语句，成功执行返回代码"10",执行失败则返回代码"11"
                                if (statement.executeUpdate(T_SQL) != 0) {
                                    printWriter.println("10");
                                } else {
                                    printWriter.println("11");
                                }
                                printWriter.flush();
                            }else{
                                break;
                            }
                        }
                    } else {
                        printWriter.println("02");
                        printWriter.flush();
                    }
                }else{
                    break;
                }
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }
}
