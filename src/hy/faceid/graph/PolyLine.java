package hy.faceid.graph;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.val;

@Value
@RequiredArgsConstructor(staticName = "from")
public class PolyLine {
    Color       color;
    List<Point> points;

    public PolyLine(@NonNull Color color, @NonNull Point... points) {
        this.color = color;
        this.points = new LinkedList<>();
        for (val point : points) if (Objects.nonNull(point)) this.points.add(point);
    }
}
