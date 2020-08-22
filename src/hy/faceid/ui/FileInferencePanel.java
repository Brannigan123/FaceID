package hy.faceid.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.io.File;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

import hy.faceid.Paths;
import hy.faceid.model.Model;
import hy.util.Image;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class FileInferencePanel extends JPanel {

    String dataFolder       = Paths.DefaultTestDataFolder.getAbsolutePath();
    String modelFolder      = Paths.DefaultModelFolder.getAbsolutePath();
    Model  model            = Model.from(modelFolder);

    String selectedFilename = "";

    public FileInferencePanel(@NonNull Consumer<String> titleSuffix) {

        setLayout(new BorderLayout(0, 0));

        val menuBar = new JMenuBar();
        val modelFolderPickerButton = new JButton("Model");
        val modelFolderChooser = new ModelDialog(null);
        val folderPathLabel = new JLabel(dataFolder);
        val dataFolderPickerButton = new JButton("...");
        val dataFolderChooser = new JFileChooser();

        val mainSplitPane = new JSplitPane();
        val filelist = new JList<>();
        val preview = new JButton("");
        val scrollableFilelist = new JScrollPane(filelist);

        val bottomPanel = new JPanel();
        val nameLabel = new JLabel("Infered");
        val inferedName = new JTextArea();

        menuBar.setMargin(new Insets(4, 4, 4, 4));
        menuBar.add(modelFolderPickerButton);
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(folderPathLabel);
        menuBar.add(Box.createHorizontalStrut(8));
        menuBar.add(dataFolderPickerButton);

        modelFolderPickerButton.setToolTipText("Open model folder");

        dataFolderPickerButton.setToolTipText("Open data folder");

        dataFolderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        dataFolderChooser.setMultiSelectionEnabled(false);

        filelist.setListData(readImagePaths());
        filelist.setToolTipText("Select entry to be infered");

        scrollableFilelist.setMinimumSize(new Dimension(200, 256));

        preview.setSize(new Dimension(mainSplitPane.getWidth() / 2, mainSplitPane.getHeight()));
        preview.setMinimumSize(new Dimension(256, 256));
        preview.setIcon(new ImageIcon());
        preview.setEnabled(true);

        mainSplitPane.setResizeWeight(0.35);
        mainSplitPane.setLeftComponent(scrollableFilelist);
        mainSplitPane.setRightComponent(preview);

        bottomPanel.add(nameLabel);
        bottomPanel.add(Box.createHorizontalGlue());
        bottomPanel.add(inferedName);

        inferedName.setText("No selection to be infered");
        inferedName.setEnabled(false);
        inferedName.setBackground(new Color(67, 73, 74));

        add(menuBar, BorderLayout.NORTH);
        add(mainSplitPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        dataFolderPickerButton.addActionListener(e -> {
            dataFolderChooser.setCurrentDirectory(new File(dataFolder));
            if (dataFolderChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                dataFolder = dataFolderChooser.getSelectedFile();
                folderPathLabel.setText(dataFolder);
                filelist.setListData(readImagePaths());
                filelist.clearSelection();
                inferedName.setText("No selection to be infered");
            }
        });

        modelFolderPickerButton.addActionListener(e -> modelFolderChooser.show());

        modelFolderChooser.setOnOk(e -> {
            try {
                val selection = modelFolderChooser.getSelectedItem();
                if (selection == null) return;
                val selectedModelPath = selection.getFile().getAbsolutePath();
                model = Model.from(selectedModelPath);
                modelFolder = selectedModelPath;
                titleSuffix.accept(getModelName());
                filelist.clearSelection();
                inferedName.setText("No selection to be infered");
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(null, "Failed to load model :\n" + exception);
            }
        });

        filelist.addListSelectionListener(e -> {
            if (!(e.getValueIsAdjusting() && filelist.isSelectionEmpty())) {
                selectedFilename = filelist.getSelectedValue();
                if (selectedFilename.isBlank()) return;
                String filepath = new File(dataFolder, selectedFilename).getAbsolutePath();
                preview.setIcon(new ImageIcon(filepath));
                try {
                    inferedName.setText(model.infer(filepath));
                } catch (Exception e2) {
                    inferedName.setText(e2.getLocalizedMessage());
                }
            }
        });

    }

    public String getModelName() { return modelFolder.substring(modelFolder.lastIndexOf(File.separatorChar) + 1); }

    private String[] readImagePaths() {
        return Stream.of(Image.paths(dataFolder)).map(path -> path.substring(path.lastIndexOf(File.separatorChar) + 1))
                .toArray(String[]::new);
    }

}
