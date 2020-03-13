package io.github.tiles;

/**
 * Created by EricLee on 2016/1/12.
 */
public class Tile {
    private int x;
    private int y;
    private int z;
    private int tileSize;

    public Tile(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Tile(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int x() {
        return x;
    }

    public Tile x(int x) {
        this.x = x;
        return this;
    }

    public int y() {
        return y;
    }

    public Tile y(int y) {
        this.y = y;
        return this;
    }

    public int z() {
        return z;
    }

    public Tile z(int z) {
        this.z = z;
        return this;
    }

    public int tileSize() {
        return tileSize;
    }

    public Tile tileSize(int tileSize) {
        this.tileSize = tileSize;
        return this;
    }

    @Override
    public String toString() {
        return "Tile{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}
