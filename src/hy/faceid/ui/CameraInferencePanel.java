package hy.faceid.ui;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import lombok.val;

public class CameraInferencePanel extends JPanel {

    // Webcam webcam = Webcam.getDefault();
    // WebcamPanel webcamPanel = new WebcamPanel(webcam);

    public CameraInferencePanel() {
        setLayout(new BorderLayout(0, 0));

        val panel = new JPanel();
        val nameLabel = new JLabel("Infered");
        val inferedName = new JTextArea();

        panel.add(nameLabel);
        panel.add(Box.createHorizontalGlue());
        panel.add(inferedName);

        inferedName.setText("male20");
        inferedName.setEnabled(false);
        inferedName.setBackground(new Color(67, 73, 74));

        // add(webcamPanel, BorderLayout.CENTER);
        add(panel, BorderLayout.SOUTH);

        /*
         * webcamPanel.addMouseListener(new MouseListener() {
         * @Override
         * public void mouseReleased(MouseEvent e) {}
         * @Override
         * public void mousePressed(MouseEvent e) {}
         * @Override
         * public void mouseExited(MouseEvent e) {}
         * @Override
         * public void mouseEntered(MouseEvent e) {}
         * @Override
         * public void mouseClicked(MouseEvent e) { webcam.getImage(); }
         * });
         */
    }

    public String getModelName() { return ""; }

}
