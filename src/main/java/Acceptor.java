import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Acceptor extends Thread {

    private Canvas canvas;

    public Acceptor(Canvas canvas) {
        this.canvas = canvas;
    }

    public void run() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(Canvas.VIEWER_PORT);
            while (true) {
                Socket peer = null;
                try {
                    peer = serverSocket.accept();
                    canvas.addViewer(peer);
                } catch (Exception e) {
                    if (!peer.isClosed())
                        try {
                            peer.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    System.out.println(e.getLocalizedMessage());
                }
            }

        } catch (Exception e) {
            if (!serverSocket.isClosed())
                try {
                    serverSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            System.out.println(e.getLocalizedMessage());
        }

    }

}
