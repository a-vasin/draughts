import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class Canvas extends JFrame {

    private static int CANVAS_WIDTH = 1000;
    private static int CANVAS_HEIGHT = 800;
    private static int BOARD_SIZE = 720;
    private static int CELLS_NUMBER = 8;
    private static int CELL_SIZE = BOARD_SIZE / CELLS_NUMBER;
    private static float LABEL_FONT_SIZE = 20.0f;
    private static float BUTTON_FONT_SIZE = 16.0f;
    private static ImageIcon boardImage = new ImageIcon("grey_board.png");


    private static Random random = new Random();

    private static int UDP_PORT = 4444;
    private static int TCP_PORT = 4445;
    public static int VIEWER_PORT = 4446;

    private static String INIT_MESSAGE = "Host a game or join to existing";
    private static String ERROR_MESSAGE = "Error occured, u can try to rehost/reconnet";
    private static String MY_MOVE_MESSAGE = "Your move";
    private static String MY_WIN_MESSAGE = "You won";
    private static String OPPONENTS_MOVE_MESSAGE = "Opponent's move";
    private static String OPPONENTS_WIN_MESSAGE = "Opponent won";
    private static String WAIT_MESSAGE = "Waiting for connection...";
    private static String FIND_MESSAGE = "Looking for server...";
    private static String WATCH_MESSAGE = "Watching game";
    private static String END_GAME_MESSAGE = "Game over";

    private JButton restart;
    private JButton joinGame;
    private JButton createGame;
    private JButton watchGame;
    private JLabel statusLabel;
    private JTextField lobbyName;
    private Board board;

    private int gameState;
    private boolean isWhite;
    private boolean isMyMove;

    private Socket peer;

    private List<Draught> white = new CopyOnWriteArrayList<Draught>();
    private List<Draught> black = new CopyOnWriteArrayList<Draught>();
    private List<Draught> highlight = new CopyOnWriteArrayList<Draught>();
    private List<Coordinate> possibleMoves = new CopyOnWriteArrayList<Coordinate>();
    private List<Draught> me;
    private List<Draught> opponent;
    private Draught selected;
    private List<Socket> newViewers = new CopyOnWriteArrayList<Socket>();
    private List<Socket> viewers = new CopyOnWriteArrayList<Socket>();

    public Canvas() {
        initUI();

        initGame();
    }

    private void initUI() {
        board = new Board();

        board.setLayout(null);

        statusLabel = new JLabel();
        statusLabel.setBounds(10, BOARD_SIZE, CANVAS_WIDTH / 2, (CANVAS_HEIGHT - BOARD_SIZE));
        statusLabel.setText(INIT_MESSAGE);
        statusLabel.setFont(statusLabel.getFont().deriveFont(LABEL_FONT_SIZE));

        board.add(statusLabel);

        createGame = new JButton("Create game");
        createGame.setFont(createGame.getFont().deriveFont(BUTTON_FONT_SIZE));
        createGame.setBounds(25, 25, (CANVAS_WIDTH - BOARD_SIZE) - 50, 75);
        createGame.addActionListener(new CreateGame());

        board.add(createGame);

        joinGame = new JButton("Join game");
        joinGame.setFont(joinGame.getFont().deriveFont(BUTTON_FONT_SIZE));
        joinGame.setBounds(25, 125, (CANVAS_WIDTH - BOARD_SIZE) - 50, 75);
        joinGame.addActionListener(new JoinGame());

        board.add(joinGame);

        watchGame = new JButton("Watch game");
        watchGame.setFont(joinGame.getFont().deriveFont(BUTTON_FONT_SIZE));
        watchGame.setBounds(25, 225, (CANVAS_WIDTH - BOARD_SIZE) - 50, 75);
        watchGame.addActionListener(new WatchGame());

        board.add(watchGame);

        restart = new JButton("Restart");
        restart.setFont(restart.getFont().deriveFont(BUTTON_FONT_SIZE));
        restart.setBounds(25, 325, (CANVAS_WIDTH - BOARD_SIZE) - 50, 75);
        restart.addActionListener(new Restart());

        board.add(restart);

        lobbyName = new JTextField("Enter password");
        lobbyName.setBounds(25, 425, (CANVAS_WIDTH - BOARD_SIZE) - 50, 25);
        lobbyName.setEnabled(true);
        //restart.addActionListener(new Restart());

        board.add(lobbyName);

        board.addMouseListener(new DraughtMouseListener());

        add(board);

        setSize(CANVAS_WIDTH, CANVAS_HEIGHT);
        setTitle("Draughts");
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void initGame() {
        createGame.setEnabled(true);
        joinGame.setEnabled(true);
        restart.setEnabled(false);
        lobbyName.setEnabled(true);

        white.clear();
        black.clear();
        for (int i = 0; i < 8; i += 2) {
            black.add(new Draught(false, (i + 1) % 8, (i + 1) / 8));
            white.add(new Draught(true, (i + 40) % 8, (i + 40) / 8));
        }
        for (int i = 8; i < 16; i += 2) {
            black.add(new Draught(false, i % 8, i / 8));
            white.add(new Draught(true, (i + 41) % 8, (i + 41) / 8));
        }
        for (int i = 16; i < 24; i += 2) {
            black.add(new Draught(false, (i + 1) % 8, (i + 1) / 8));
            white.add(new Draught(true, (i + 40) % 8, (i + 40) / 8));
        }

        gameState = 0;

        if (random.nextInt(2) == 0) {
            me = white;
            opponent = black;
            isWhite = true;
            isMyMove = true;
        } else {
            me = black;
            opponent = white;
            isWhite = false;
            isMyMove = false;
        }
    }

    public void addViewer(Socket peer) {
        newViewers.add(peer);
    }

    private void sendToViewers(Draught oldDraught, Draught newDraught) {
        for (Socket viewer : viewers) {
            try {
                sendDraught(viewer, oldDraught);
                sendDraught(viewer, newDraught);
            } catch (Exception e) {
                viewers.remove(viewer);
                System.out.println(e.getLocalizedMessage());
            }
        }

        synchronized (newViewers) {
            for (Socket viewer : newViewers) {
                try {
                    sendGameState(viewer);
                    viewers.add(viewer);
                } catch (Exception e) {
                    System.out.println(e.getLocalizedMessage());
                }
            }
            newViewers.clear();
        }

    }


    private class Board extends JPanel implements Runnable {
        private int DELAY = 17;

        private Thread animator;

        public Board() {
            super();
            animator = new Thread(this);
            animator.start();
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);

            g.drawImage(boardImage.getImage(), CANVAS_WIDTH - BOARD_SIZE, 0, BOARD_SIZE, BOARD_SIZE, this);

            drawHighlights(g);
            drawPossibleMoves(g);

            drawDraughts(g);
        }

        private void drawHighlights(Graphics g) {
            g.setColor(new Color(0, 255, 0, 64));
            for (Draught draught : highlight)
                g.fillRect(CANVAS_WIDTH - BOARD_SIZE + draught.getX() * CELL_SIZE, draught.getY() * CELL_SIZE, CELL_SIZE, CELL_SIZE);
        }

        private void drawPossibleMoves(Graphics g) {
            g.setColor(new Color(255, 0, 0, 64));
            for (Coordinate coordinate : possibleMoves)
                g.fillRect(CANVAS_WIDTH - BOARD_SIZE + coordinate.getX() * CELL_SIZE, coordinate.getY() * CELL_SIZE, CELL_SIZE, CELL_SIZE);
        }

        private void drawDraughts(Graphics g) {
            for (Draught draught : white) {
                g.drawImage(draught.getImage(), CANVAS_WIDTH - BOARD_SIZE + draught.getX() * CELL_SIZE, draught.getY() * CELL_SIZE, CELL_SIZE, CELL_SIZE, this);
            }
            for (Draught draught : black) {
                g.drawImage(draught.getImage(), CANVAS_WIDTH - BOARD_SIZE + draught.getX() * CELL_SIZE, draught.getY() * CELL_SIZE, CELL_SIZE, CELL_SIZE, this);
            }
        }

        @Override
        public void run() {
            long beforeTime, timeDiff, sleep;

            beforeTime = System.currentTimeMillis();
            while (true) {
                repaint();

                timeDiff = System.currentTimeMillis() - beforeTime;
                sleep = DELAY - timeDiff;

                if (sleep < 0)
                    sleep = 2;
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    System.out.println("interrupted");
                }
                beforeTime = System.currentTimeMillis();
            }
        }
    }

    private void handlerError() {
        gameState = 0;
        statusLabel.setText(ERROR_MESSAGE);
        createGame.setEnabled(true);
        joinGame.setEnabled(true);
        restart.setEnabled(true);
        lobbyName.setEnabled(true);
        highlight.clear();
        selected = null;
        possibleMoves.clear();
        Util.closeSocket(peer);
        peer = null;
        closeViewers();
    }

    private Draught readDraught(InputStream is, boolean isWhite) throws IOException {
        boolean isPromoted;
        if (readBytes(is, 1)[0] == 0)
            isPromoted = false;
        else
            isPromoted = true;
        int x = new BigInteger(readBytes(is, 4)).intValue();
        int y = new BigInteger(readBytes(is, 4)).intValue();
        Draught draught = new Draught(isWhite, x, y);
        if (isPromoted)
            draught.promote();
        return draught;
    }

    private byte[] readBytes(InputStream is, int size) throws IOException {
        byte[] buf = new byte[size];
        if (is.read(buf) != size)
            throw new IllegalArgumentException("Unexpected read result");
        return buf;
    }

    private void handleOpponentMove(Socket socket) throws IOException {
        Draught oldDraught = readDraught(socket.getInputStream(), !isWhite);
        Draught newDraught = readDraught(socket.getInputStream(), !isWhite);
        opponent.remove(oldDraught);
        opponent.add(newDraught);
        sendToViewers(oldDraught, newDraught);
        if (handleDelete(me, oldDraught, newDraught) && !getAggressiveMoves(getGameMatrix(false), newDraught).isEmpty())
            handleOpponentMove(socket);

    }

    private boolean handleDelete(List<Draught> list, Draught oldDraught, Draught newDraught) {
        if (Math.abs(oldDraught.getX() - newDraught.getX()) == 2) {
            int x = (newDraught.getX() + oldDraught.getX()) / 2;
            int y = (newDraught.getY() + oldDraught.getY()) / 2;
            Draught toDelete = null;
            for (Draught draught : list) {
                if (draught.getX() == x && draught.getY() == y) {
                    toDelete = draught;
                    break;
                }
            }
            list.remove(toDelete);
            return true;
        }
        return false;
    }

    private int[][] getGameMatrix(boolean isMe) {
        int[][] matrix = new int[CELLS_NUMBER][CELLS_NUMBER];
        for (Draught draught : me) {
            matrix[draught.getX()][draught.getY()] = isMe ? 1 : 2;
        }
        for (Draught draught : opponent) {
            matrix[draught.getX()][draught.getY()] = isMe ? 2 : 1;
        }
        return matrix;
    }

    private List<Coordinate> getAggressiveMoves(int[][] matrix, Draught draught) {
        List<Coordinate> result = new CopyOnWriteArrayList<Coordinate>();
        int x = draught.getX();
        int y = draught.getY();
        int[][] moves = draught.getMoves();
        for (int i = 0; i < moves.length; ++i) {
            int moveX = x + moves[i][0];
            int moveY = y + moves[i][1];
            if (isConsistent(moveX, moveY)) {
                if (matrix[moveX][moveY] == 2)
                    if (isConsistent(moveX + moves[i][0], moveY + moves[i][1]) && matrix[moveX + moves[i][0]][moveY + moves[i][1]] == 0) {
                        Coordinate coordinate = new Coordinate(moveX + moves[i][0], moveY + moves[i][1]);
                        if (!result.contains(coordinate))
                            result.add(coordinate);
                    }
            }
        }
        return result;
    }

    private List<Coordinate> getPassiveMoves(int[][] matrix, Draught draught) {
        List<Coordinate> result = new CopyOnWriteArrayList<Coordinate>();
        int x = draught.getX();
        int y = draught.getY();
        int[][] moves = draught.getMoves();
        for (int i = 0; i < moves.length; ++i) {
            int moveX = x + moves[i][0];
            int moveY = y + moves[i][1];
            if (isConsistent(moveX, moveY)) {
                if (matrix[moveX][moveY] == 0) {
                    Coordinate coordinate = new Coordinate(moveX, moveY);
                    if (!result.contains(coordinate))
                        result.add(coordinate);
                }
            }
        }
        return result;
    }

    private void setHighlighted() {
        int[][] matrix = getGameMatrix(true);
        List<Draught> aggressive = new ArrayList<Draught>();
        List<Draught> passive = new ArrayList<Draught>();
        for (Draught draught : me) {
            if (!getAggressiveMoves(matrix, draught).isEmpty())
                aggressive.add(draught);
            if (!getPassiveMoves(matrix, draught).isEmpty())
                passive.add(draught);
        }
        if (aggressive.isEmpty())
            highlight = passive;
        else
            highlight = aggressive;
    }

    private void setPossibleMoves() {
        int[][] matrix = getGameMatrix(true);
        List<Coordinate> aggressive = getAggressiveMoves(matrix, selected);
        List<Coordinate> passive = getPassiveMoves(matrix, selected);
        if (aggressive.isEmpty())
            possibleMoves = passive;
        else
            possibleMoves = aggressive;
    }

    private boolean isConsistent(int x, int y) {
        return x >= 0 && x < CELLS_NUMBER && y >= 0 && y < CELLS_NUMBER;
    }

    private void closeViewers() {
        for (Socket viewer : newViewers)
            Util.closeSocket(viewer);
        newViewers.clear();
        for (Socket viewer : viewers)
            Util.closeSocket(viewer);
        viewers.clear();
    }

    private boolean finished() {
        if (me.isEmpty()) {
            gameState = 0;
            statusLabel.setText(OPPONENTS_WIN_MESSAGE);
            createGame.setEnabled(false);
            joinGame.setEnabled(false);
            restart.setEnabled(true);
            watchGame.setEnabled(true);
            Util.closeSocket(peer);
            peer = null;
            closeViewers();
            return true;
        }

        if (opponent.isEmpty()) {
            gameState = 0;
            statusLabel.setText(MY_WIN_MESSAGE);
            createGame.setEnabled(false);
            joinGame.setEnabled(false);
            restart.setEnabled(true);
            watchGame.setEnabled(true);
            Util.closeSocket(peer);
            peer = null;
            closeViewers();
            return true;
        }

        return false;
    }

    private class CreateGame implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            new Thread() {
                public void run() {
                    createGame.setEnabled(false);
                    joinGame.setEnabled(false);
                    restart.setEnabled(false);
                    lobbyName.setEnabled(false);
                    watchGame.setEnabled(false);
                    Acceptor acceptor = new Acceptor(Canvas.this);
                    acceptor.start();
                    Announcer announcer = new Announcer(lobbyName.getText());
                    announcer.start();
                    statusLabel.setText(WAIT_MESSAGE);
                    try {
                        ServerSocket serverSocket = new ServerSocket(TCP_PORT);
                        while (true) {
                            try {
                                peer = serverSocket.accept();
                                System.out.println("Received connection from :" + peer.getInetAddress().getHostAddress());
                                if (isMyMove)
                                    statusLabel.setText(MY_MOVE_MESSAGE);
                                else
                                    statusLabel.setText(OPPONENTS_MOVE_MESSAGE);

                                sendGameState(peer);

                                if (!isMyMove) {
                                    handleOpponentMove(peer);
                                    isMyMove = true;
                                    if (finished())
                                        return;
                                    statusLabel.setText(MY_MOVE_MESSAGE);
                                }
                                gameState = 1;
                                setHighlighted();
                                break;
                            } catch (Exception ex) {
                                if (!announcer.isInterrupted())
                                    announcer.interrupt();
                                announcer = new Announcer(lobbyName.getText());
                                announcer.start();
                                System.out.println();
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println(ex.getLocalizedMessage());
                        statusLabel.setText(ERROR_MESSAGE);
                        handlerError();
                    } finally {
                        if (!announcer.isInterrupted())
                            announcer.interrupt();
                    }
                }
            }.start();
        }
    }

    private void sendGameState(Socket peer) throws IOException {
        OutputStream os = peer.getOutputStream();
        os.write(isWhite ? 0 : 1);
        os.write(isMyMove ? 0 : 1);
        os.write(Util.intToByte(white.size()));
        for (Draught draught : white)
            sendDraught(peer, draught);
        os.write(Util.intToByte(black.size()));
        for (Draught draught : black)
            sendDraught(peer, draught);
    }

    private void sendDraught(Socket peer, Draught draught) throws IOException {
        OutputStream os = peer.getOutputStream();
        os.write(draught.isPromoted() ? 1 : 0);
        os.write(Util.intToByte(draught.getX()));
        os.write(Util.intToByte(draught.getY()));
    }

    private void getGame() throws IOException {
        InputStream is = peer.getInputStream();
        int color = new BigInteger(readBytes(is, 1)).intValue();
        int move = new BigInteger(readBytes(is, 1)).intValue();
        int whiteSize = new BigInteger(readBytes(is, 4)).intValue();
        List<Draught> newWhite = new CopyOnWriteArrayList<Draught>();
        for (int i = 0; i < whiteSize; ++i) {
            newWhite.add(readDraught(is, true));
        }
        int blackSize = new BigInteger(readBytes(is, 4)).intValue();
        List<Draught> newBlack = new CopyOnWriteArrayList<Draught>();
        for (int i = 0; i < blackSize; ++i) {
            newBlack.add(readDraught(is, false));
        }
        white = newWhite;
        black = newBlack;
        isWhite = color == 1;
        isMyMove = move == 1;

    }

    private void initDraughtsSets() {
        if (isWhite) {
            me = white;
            opponent = black;
        } else {
            me = black;
            opponent = white;
        }
    }

    private class JoinGame implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            new Thread() {
                public void run() {
                    createGame.setEnabled(false);
                    joinGame.setEnabled(false);
                    restart.setEnabled(false);
                    lobbyName.setEnabled(false);
                    watchGame.setEnabled(false);
                    statusLabel.setText(FIND_MESSAGE);
                    MulticastSocket socket = null;
                    try {
                        socket = new MulticastSocket(UDP_PORT);
                        while (true) {
                            try {
                                DatagramPacket packet;
                                byte[] buf = new byte[256];
                                packet = new DatagramPacket(buf, buf.length);
                                socket.receive(packet);

                                int length = new BigInteger(Arrays.copyOfRange(packet.getData(), 0, 4)).intValue();

                                String message = new String(Arrays.copyOfRange(packet.getData(), 4, 4 + length), "UTF-8");

                                if (!message.equals(lobbyName.getText()))
                                    continue;

                                peer = new Socket(packet.getAddress(), TCP_PORT);
                                getGame();

                                initDraughtsSets();

                                if (isMyMove)
                                    statusLabel.setText(MY_MOVE_MESSAGE);
                                else
                                    statusLabel.setText(OPPONENTS_MOVE_MESSAGE);

                                if (!isMyMove) {
                                    handleOpponentMove(peer);
                                    isMyMove = true;
                                    if (finished())
                                        return;
                                    statusLabel.setText(MY_MOVE_MESSAGE);
                                }

                                gameState = 1;
                                setHighlighted();
                                break;

                            } catch (Exception ex) {
                                System.err.println(ex.getLocalizedMessage());
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println(ex.getLocalizedMessage());
                        handlerError();
                    }
                    socket.close();
                }
            }.start();
        }
    }

    private class Restart implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            initGame();
        }
    }

    private class WatchGame implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            new Thread() {
                public void run() {
                    createGame.setEnabled(false);
                    joinGame.setEnabled(false);
                    restart.setEnabled(false);
                    watchGame.setEnabled(false);
                    lobbyName.setEnabled(false);
                    statusLabel.setText(FIND_MESSAGE);
                    MulticastSocket socket = null;
                    try {
                        socket = new MulticastSocket(UDP_PORT);
                        while (true) {
                            try {
                                DatagramPacket packet;
                                byte[] buf = new byte[256];
                                packet = new DatagramPacket(buf, buf.length);
                                socket.receive(packet);

                                int length = new BigInteger(Arrays.copyOfRange(packet.getData(), 0, 4)).intValue();

                                String message = new String(Arrays.copyOfRange(packet.getData(), 4, 4 + length), "UTF-8");

                                if (!message.equals(lobbyName.getText()))
                                    continue;

                                statusLabel.setText(WATCH_MESSAGE);

                                peer = new Socket(packet.getAddress(), VIEWER_PORT);
                                getGame();

                                if (isMyMove) {
                                    isWhite = !isWhite;
                                }

                                initDraughtsSets();

                                while (true) {
                                    if (finished()) {
                                        statusLabel.setText(END_GAME_MESSAGE);
                                        return;
                                    }

                                    isWhite = !isWhite;

                                    List<Draught> tmp = me;
                                    me = opponent;
                                    opponent = tmp;

                                    handleOpponentMove(peer);

                                }

                            } catch (Exception ex) {
                                System.err.println(ex.getLocalizedMessage());
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println(ex.getLocalizedMessage());
                        handlerError();
                    }
                    socket.close();
                }
            }.start();
        }
    }

    private class DraughtMouseListener implements MouseListener {

        @Override
        public void mouseClicked(MouseEvent e) {

        }

        private Draught searchSelected(List<Draught> list, int x, int y) {
            for (Draught draught : list)
                if (draught.getX() == x && draught.getY() == y) {
                    return draught;
                }
            return null;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            final int x = (e.getX() - (CANVAS_WIDTH - BOARD_SIZE)) / CELL_SIZE;
            final int y = e.getY() / CELL_SIZE;
            new Thread() {
                public void run() {
                    if (!isConsistent(x, y))
                        return;
                    if (gameState == 1 || gameState == 2) {
                        Draught found = searchSelected(me, x, y);
                        if (found != null && highlight.contains(found)) {
                            selected = found;
                            setPossibleMoves();
                            gameState = 2;
                            return;
                        }
                    }
                    if (gameState == 2) {
                        for (Coordinate coordinate : possibleMoves) {
                            if (coordinate.getX() == x && coordinate.getY() == y) {
                                try {
                                    Draught oldDraught = selected.clone();
                                    selected.move(coordinate);
                                    sendDraught(peer, oldDraught);
                                    sendDraught(peer, selected);

                                    sendToViewers(oldDraught, selected);

                                    highlight.clear();
                                    possibleMoves.clear();

                                    if (handleDelete(opponent, oldDraught, selected) && !getAggressiveMoves(getGameMatrix(true), selected).isEmpty()) {
                                        gameState = 1;
                                        highlight.add(selected);
                                        return;
                                    }

                                    if (finished())
                                        return;

                                    isMyMove = false;
                                    statusLabel.setText(OPPONENTS_MOVE_MESSAGE);
                                    handleOpponentMove(peer);

                                    if (finished())
                                        return;

                                    isMyMove = true;
                                    statusLabel.setText(MY_MOVE_MESSAGE);

                                    gameState = 1;
                                    setHighlighted();
                                    possibleMoves.clear();
                                } catch (Exception e1) {
                                    handlerError();
                                    System.err.println(e1.getLocalizedMessage());
                                }
                            }
                        }
                    }
                }

            }.start();

        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }
    }

    public static void main(String[] args) {
        new Canvas();
    }
}
