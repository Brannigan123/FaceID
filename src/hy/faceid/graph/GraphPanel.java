package hy.faceid.graph;

import java.awt.Graphics;

import javax.swing.JPanel;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import lombok.experimental.FieldDefaults;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class GraphPanel extends JPanel {
    @Getter Graph graph;

    public GraphPanel(String xLabel, String yLabel, int padding, int width, int height) {
        graph = new Graph(xLabel, yLabel, padding, width, height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        val w = getWidth(), h = getHeight();
        graph.setWidth(w);
        graph.setHeight(h);
        graph.draw();
        g.drawImage(graph.getGraph(), 0, 0, w, h, 0, 0, w, h, null);
    }

    public void setCapacity(int capcity) { graph.setCapacity(capcity); }

    public void add(String name, PolyLine line) {
        graph.add(name, line);
        repaint();
    }

    public void add(String name, Point point) {
        graph.add(name, point);
        repaint();
    }

    public void remove(String name) {
        graph.remove(name);
        repaint();
    }

    public void clear() { graph.clear(); }

}
