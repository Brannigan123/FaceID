package hy.faceid.graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;

import javax.imageio.ImageIO;
import hy.util.Format;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Graph {
    String                    xLabel;
    String                    yLabel;
    int                       padding;
    @Getter int               width;
    @Getter int               height;
    @Getter int               capacity;

    @Getter BufferedImage     graph;
    @Getter Graphics2D        graphics;

    HashMap<String, PolyLine> polylines;
    double                    minX        = 0;
    double                    maxX        = 1;
    double                    xInterval   = 1;
    double                    minY        = 0;
    double                    maxY        = 1;
    double                    yInterval   = 1;

    boolean                   dataChanged = true;
    boolean                   draw        = true;

    public Graph(@NonNull String xLabel, @NonNull String yLabel, int padding, int width, int height) {
        this.xLabel = xLabel;
        this.yLabel = yLabel;
        this.padding = padding + 26;
        this.width = width;
        this.height = height;
        this.capacity = Integer.MAX_VALUE;
        this.graph = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        this.graphics = this.graph.createGraphics();
        this.polylines = new HashMap<>();
    }

    public Graph add(@NonNull String name, @NonNull PolyLine line) {
        polylines.put(name, line);
        dataChanged = true;
        draw = true;
        return this;
    }

    public Graph add(@NonNull String name, @NonNull Point point) {
        val points = polylines.computeIfAbsent(name, n -> new PolyLine(point.getColor())).getPoints();
        while (points.size() >= capacity) points.remove(0);
        points.add(point);
        dataChanged = true;
        draw = true;
        return this;
    }

    public Graph remove(@NonNull String name) {
        polylines.remove(name);
        dataChanged = true;
        draw = true;
        return this;
    }

    public Graph setWidth(int width) {
        if (width != this.width) {
            this.width = width;
            draw = true;
        }
        return this;
    }

    public Graph setHeight(int height) {
        if (height != this.height) {
            this.height = height;
            draw = true;
        }
        return this;
    }

    public Graph setCapacity(int capcity) {
        this.capacity = Math.abs(capcity);
        return this;
    }

    public void draw() {
        if (dataChanged) {
            findRange();
            dataChanged = false;
        }
        if (draw) {
            dispose();
            graph = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            graphics = this.graph.createGraphics();
            drawAxes();
            drawTicks();
            drawLines();
            draw = false;
        }
    }

    public void dispose() { graphics.dispose(); }

    private void findRange() {
        val xStats = polylines.values().stream()
                .flatMapToDouble(line -> line.getPoints().stream().mapToDouble(Point::getX)).summaryStatistics();
        val yStats = polylines.values().stream()
                .flatMapToDouble(line -> line.getPoints().stream().mapToDouble(Point::getY)).summaryStatistics();
        if (xStats.getCount() > 0) {
            if (xStats.getCount() == 1) {
                minX = xStats.getMin() - 1;
                maxX = xStats.getMax() + 1;
                return;
            }
            minX = xStats.getMin();
            maxX = xStats.getMax();
            xInterval = maxX - minX;
        }
        if (yStats.getCount() > 0) {
            if (yStats.getCount() == 1) {
                minY = yStats.getMin() - 1;
                maxY = yStats.getMax() + 1;
                return;
            }
            minY = yStats.getMin();
            maxY = yStats.getMax();
            yInterval = maxY - minY;
        }
    }

    private void drawAxes() {
        graphics.setColor(Color.lightGray);
        graphics.setStroke(new BasicStroke(1));
        graphics.drawLine(padding, height - padding, width - padding, height - padding);
        graphics.drawLine(padding, height - padding, padding, padding);
        graphics.setFont(graphics.getFont().deriveFont(16.0f));
        graphics.drawString(xLabel, width / 2 - 5 * xLabel.length(), height - padding + 30);
        val oldTransform = graphics.getTransform();
        graphics.translate(padding - 24, height / 2 + 5 * yLabel.length());
        graphics.rotate(Math.toRadians(-90.0));
        graphics.drawString(yLabel, 0, 0);
        graphics.setTransform(oldTransform);
    }

    private void drawTicks() {
        graphics.setFont(graphics.getFont().deriveFont(12.0f));
        val nonPadWidth = width - 2 * padding, nonPadHeight = height - 2 * padding;
        val tickSpacing = Math.max(40, Math.min(Math.min(80, nonPadWidth), Math.min(80, nonPadHeight)));
        val xTicks = nonPadWidth / tickSpacing, yTicks = nonPadHeight / tickSpacing;
        if (xTicks > 0) for (var i = 0; i <= xTicks; i++) {
            graphics.drawLine(padding + tickSpacing * i, height - padding - 2, padding + tickSpacing * i,
                height - padding + 2);
            graphics.drawString(Format.toString(minX + xInterval / xTicks * i), padding + tickSpacing * i,
                height - padding + 12);
        }
        if (yTicks > 0) for (var i = 0; i <= yTicks; i++) {
            graphics.drawLine(padding - 2, height - padding - tickSpacing * i, padding + 2,
                height - padding - tickSpacing * i);
            val str = Format.toString(minY + yInterval / yTicks * i);
            graphics.drawString(str, padding - 12 - str.length(), height - padding - tickSpacing * i);
        }
    }

    private void drawLines() {
        for (val line : polylines.entrySet()) {
            val color = line.getValue().getColor();
            val points = line.getValue().getPoints();
            Point previousPoint = null;
            for (val point : points) {
                graphics.setColor(point.getColor());
                graphics.fillOval(scaleX(point.getX()) - 2, scaleY(point.getY()) - 2, 4, 4);
                if (previousPoint != null) {
                    graphics.setColor(color);
                    graphics.drawLine(scaleX(previousPoint.getX()), scaleY(previousPoint.getY()), scaleX(point.getX()),
                        scaleY(point.getY()));
                }
                previousPoint = point;
            }
        }
    }

    private int scaleX(double x) { return padding + (int) ((x - minX) / xInterval * (width - 2 * padding)); }

    private int scaleY(double y) { return height - padding - (int) ((y - minY) / yInterval * (height - 2 * padding)); }

    @SneakyThrows
    public void save(String type, String path) { ImageIO.write(graph, type, new File(path)); }

    public void clear() { polylines.clear(); }
}
