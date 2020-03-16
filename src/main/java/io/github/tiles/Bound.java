package io.github.tiles;

public class Bound {
    private double xmin = 0.0;
    private double ymin = 0.0;
    private double xmax = 0.0;
    private double ymax = 0.0;

    public Bound(double minx, double miny, double maxx, double maxy) {
        xmin = Math.min(minx, maxx);
        xmax = Math.max(minx, maxx);
        ymin = Math.min(miny, maxy);
        ymax = Math.max(miny, maxy);
    }

    @Override
    public String toString() {
        return xmin + "," + ymin + "," + xmax + "," + ymax;
    }

    public double xmin() {
        return xmin;
    }

    public double ymin() {
        return ymin;
    }

    public double xmax() {
        return xmax;
    }

    public double ymax() {
        return ymax;
    }
}