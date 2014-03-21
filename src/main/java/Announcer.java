import com.sun.deploy.util.ArrayUtil;
import org.apache.commons.lang.ArrayUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;

public class Announcer extends Thread {
    public Announcer(String message) {
        this.message = message;
    }

    private String message;

    private static final long SECOND = 1000;

    private static int UDP_PORT = 4444;
    private static String IP = "255.255.255.255";

    public void run() {
        if (Thread.currentThread().isAlive()) {
            new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        DatagramSocket clientSocket = new DatagramSocket();

                        byte[] buffer = Util.intToByte(message.length());
                        buffer = ArrayUtils.addAll(buffer, message.getBytes("UTF-8"));

                        InetAddress group = InetAddress.getByName(IP);
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, UDP_PORT);
                        clientSocket.send(packet);

                        clientSocket.close();
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }
                }
            }, 0, SECOND);
        }
    }
}
