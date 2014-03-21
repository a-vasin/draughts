import javax.swing.*;
import java.awt.*;

public class Draught {
    private static Image white = (new ImageIcon("white.png")).getImage();
    private static Image black = (new ImageIcon("black.png")).getImage();
    private static Image promotedWhite = (new ImageIcon("promotedWhite.png")).getImage();
    private static Image promotedBlack = (new ImageIcon("promotedBlack.png")).getImage();
    private static int[][] whiteMoves = {{1, -1}, {-1, -1}};
    private static int[][] blackMoves = {{1, 1}, {-1, 1}};
    private static int[][] allMoves = {{1, 1}, {-1, 1}, {1, -1}, {-1, -1}};

    private int x;
    private int y;
    private boolean isWhite;
    private boolean isPromoted;

    public Draught(boolean isWhite, int x, int y) {
        this.x = x;
        this.y = y;
        this.isPromoted = false;
        this.isWhite = isWhite;
    }

    public Image getImage() {
        if (isWhite) {
            if (isPromoted)
                return promotedWhite;
            else
                return white;
        } else {
            if (isPromoted)
                return promotedBlack;
            else
                return black;
        }
    }

    public int getY() {
        return y;
    }

    public int getX() {

        return x;
    }

    public boolean isPromoted() {
        return isPromoted;
    }

    public void promote() {
        isPromoted = true;
    }

    public boolean isWhite() {
        return isWhite;
    }

    public int[][] getMoves() {
        if (isPromoted)
            return allMoves;
        else if (isWhite)
            return whiteMoves;
        else
            return blackMoves;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Draught draught = (Draught) o;

        if (isPromoted != draught.isPromoted) return false;
        if (isWhite != draught.isWhite) return false;
        if (x != draught.x) return false;
        if (y != draught.y) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + (isWhite ? 1 : 0);
        result = 31 * result + (isPromoted ? 1 : 0);
        return result;
    }

    public void move(Coordinate coordinate) {
        this.x = coordinate.getX();
        this.y = coordinate.getY();
        if (y == 7 || y == 0)
            promote();
    }

    @Override
    public Draught clone() {
        Draught newDraught = new Draught(this.isWhite, this.x, this.y);
        if (isPromoted())
            newDraught.promote();
        return newDraught;
    }
}
