
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;


public class WebSocketService extends WebSocketServer {
    //todo 还要判断哪个客户端 有可能用户通过手机和PC同时打开


    //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    //private static int onlineCount = 0;
    //concurrent包的线程安全Map，用来存放每个客户端对应的WebSocket对象。若要实现服务端与单一客户端通信的话，可以使用Map来存放，其中Key可以为用户标识
    //private static Map<String, WebSocket> clients = new ConcurrentHashMap<String, WebSocket>();
    private static ConcurrentHashMap<String, JSONObject> editlist = new ConcurrentHashMap<String, JSONObject>();
    private static ConcurrentHashMap<String, JSONObject> readlist = new ConcurrentHashMap<String, JSONObject>();

    public static int port = 8887;
    public static int timeout = 10000;

    public WebSocketService() throws UnknownHostException {
        super(new InetSocketAddress(port));
        // TODO Auto-generated constructor stub
    }

    //public static synchronized Map<String, WebSocket> getClients() {
    //    return clients;
   //}
    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        // TODO Auto-generated method stub

        System.out.println("Open: online="+this.getConnections().size());
        //System.out.println("New connection from " + webSocket.getRemoteSocketAddress().getAddress().getHostAddress());
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        // TODO Auto-generated method stub
        System.out.println(webSocket.getSSLSession());
        System.out.println("Close     "+i+" - "+s+" - "+b);
        System.out.println("Close: online="+this.getConnections().size());
    }

    @Override
    public void onMessage(WebSocket webSocket, String msg) {
        //System.out.println("Message from " + webSocket.getRemoteSocketAddress().getAddress().getHostAddress());
        System.out.println(System.currentTimeMillis() +" Content: " + msg);
        JSONObject json = JSON.parseObject(msg);
        JSONObject result = new JSONObject();
        json.put("timestamp",System.currentTimeMillis());
        if (json.getString("action").equals("edit")){
            JSONObject client = editlist.get(json.getString("unid"));
            //判断是否包含在在线页面清单里
            if (client != null) {
                //已有打开记录,则判断当前打开人与上一次打开人是否相同
                if (json.getString("user").equals(client.getString("user"))){
                    //用新的页面对像更新
                    //Long tsnew = Long.parseLong(json.getString("timestamp"));
                    //Long tsold = Long.parseLong(client.getString("timestamp"));
                    editlist.put(json.getString("unid"),json);

                    result.put("status","2");   //续订
                    result.put("msg","renew");
                    result.put("editlist",client.getString("user"));   //锁定人员(编辑人员)
                    result.put("readlist","");
                    webSocket.send(result.toJSONString());

                }else{
                    //如果不是同一人,则要判断时间 如果超时N秒,则由将老用户T下线,以新用户代替
                    Long tsnew = Long.parseLong(json.getString("timestamp"));
                    Long tsold = Long.parseLong(client.getString("timestamp"));
                    if (tsnew - tsold >timeout){
                        //上一次声明超过timeout时间时,新用户获取锁
                        editlist.put(json.getString("unid"),json);
                        result.put("status","3");   //抢占成功
                        result.put("msg","grab");
                        result.put("editlist",client.getString("user"));   //已超时的原锁定人员(编辑人员)
                        result.put("readlist","");
                     }else{
                        result.put("status","-1");   //被锁定中
                        result.put("msg","locked");
                        result.put("editlist",client.getString("user"));   //锁定人员(编辑人员)
                        result.put("readlist","");
                    }

                    webSocket.send(result.toJSONString());

                }


            }else{
                //如果此页还没有打开,则记录
                editlist.put(json.getString("unid"),json);
                result.put("status","1");   //登记成功
                result.put("msg","regist");
                result.put("editlist",json.getString("user"));   //锁定人员(编辑人员)
                result.put("readlist","");
                webSocket.send(result.toJSONString());

            }

        }else if (json.getString("action").equals("read")){
            result.put("status","0");   //read
            result.put("msg","read");
            JSONObject edit = editlist.get(json.getString("unid"));
            if (edit != null) {
                result.put("editlist",edit.getString("user"));   //锁定人员(编辑人员)
            }

            result.put("readlist","");
            webSocket.send(result.toJSONString());
        }else{
            //
            result.put("status","");   //read
            result.put("msg","no param");   //read
            webSocket.send(result.toJSONString());
        }



        // TODO Auto-generated method stub
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        // TODO Auto-generated method stub
        System.out.println("ERROR from " + webSocket.getRemoteSocketAddress().getAddress().getHostAddress());
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket Service Start...");
    }

    /*
    public static synchronized int getOnlineCount() {

        return onlineCount;
    }
    public static synchronized void addOnlineCount() {
        WebSocketService.onlineCount++;
    }
    public static synchronized void subOnlineCount() {
        WebSocketService.onlineCount--;
    }
  */
    /**
     * @param args
     * @throws UnknownHostException
     */
    public static void main(String[] args) throws UnknownHostException {
        // TODO Auto-generated method stub
        WebSocketService server = new WebSocketService();
        server.start();
        System.out.println("Server start at "+server.getAddress());
    }
}