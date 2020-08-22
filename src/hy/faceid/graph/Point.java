package hy.faceid.graph;

import java.awt.Color;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(staticName = "at")
public class Point {
    @NonNull Color color;
    double         x;
    double         y;
}
