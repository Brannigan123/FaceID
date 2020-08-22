package hy.faceid.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import com.formdev.flatlaf.FlatDarkLaf;

import lombok.val;

public class Main {

    private JFrame frame;

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    FlatDarkLaf.install();
                    val window = new Main();
                    window.frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public Main() { initialize(); }

    private void initialize() {
        frame = new JFrame();
        val tabbedPane = new JTabbedPane(JTabbedPane.TOP);

        val cameraInferencePanel = new CameraInferencePanel();
        val fileInferencePanel = new FileInferencePanel(this::appendTitle);
        val trainingPanel = new TrainingPanel(this::appendTitle);

        frame.setTitle("Face ID");
        frame.setSize(700, 600);
        frame.setMinimumSize(new Dimension(500, 400));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);

        tabbedPane.addTab("Camera Inference", cameraInferencePanel);
        tabbedPane.addTab("File Inference", fileInferencePanel);
        tabbedPane.addTab("Training", trainingPanel);

        tabbedPane.addChangeListener(e -> {
            val selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex == 0) appendTitle("");
            else if (selectedIndex == 1) appendTitle(fileInferencePanel.getModelName());
            else if (selectedIndex == 2) appendTitle(trainingPanel.getModelName());
        });
    }

    private void appendTitle(String suffix) {
        frame.setTitle("FaceID | " + suffix);
    }

}
