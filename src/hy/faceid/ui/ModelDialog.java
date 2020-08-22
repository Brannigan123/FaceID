package hy.faceid.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import hy.faceid.Paths;
import hy.faceid.model.Model;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.val;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ModelDialog {

    Supplier<List<String>> categories;

    JScrollPane            listScroll;
    JList<Entry>           list;
    JOptionPane            optionPane;
    JButton                newButton, okButton, cancelButton;
    ActionListener         okEvent, cancelEvent;
    JDialog                dialog;

    public ModelDialog(Supplier<List<String>> categoriesSupplier) {
        list = new JList<>();
        listScroll = new JScrollPane(list);
        categories = categoriesSupplier;
        createAndDisplayOptionPane();
    }

    private void createAndDisplayOptionPane() {
        setupButtons();
        JPanel pane = layoutComponents();
        optionPane = new JOptionPane(pane);
        optionPane.setOptions(categories == null ? new Object[] { okButton, cancelButton }
                : new Object[] { newButton, okButton, cancelButton });
        dialog = optionPane.createDialog("Model Selection");
        dialog.setMinimumSize(new Dimension(300, 300));
        dialog.setResizable(true);
    }

    private void setupButtons() {
        newButton = new JButton("New");
        newButton.addActionListener(e -> handleNewButtonClick(e));

        okButton = new JButton("Ok");
        okButton.addActionListener(e -> handleOkButtonClick(e));

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> handleCancelButtonClick(e));
    }

    private JPanel layoutComponents() {
        val panel = new JPanel(new BorderLayout(4, 4));
        panel.add(new JLabel("Create a new model or select from the existing model"), BorderLayout.NORTH);
        panel.add(listScroll, BorderLayout.CENTER);
        return panel;
    }

    public void setOnOk(ActionListener event) { okEvent = event; }

    public void setOnClose(ActionListener event) { cancelEvent = event; }

    private void handleNewButtonClick(ActionEvent e) {
        val newModelName = JOptionPane.showInputDialog("Enter name for new model", "new model");
        try {
            if (Model.createNewModel(newModelName, categories.get())) show();
        } catch (NullPointerException exception) {
            JOptionPane.showMessageDialog(null,
                "Failed to create model '" + newModelName + "'.\n" + exception.getMessage(), "Model creation failed",
                JOptionPane.WARNING_MESSAGE);
        }
    }

    private void handleOkButtonClick(ActionEvent e) {
        if (okEvent != null) { okEvent.actionPerformed(e); }
        hide();
    }

    private void handleCancelButtonClick(ActionEvent e) {
        if (cancelEvent != null) { cancelEvent.actionPerformed(e); }
        hide();
    }

    public void show() {
        list.setListData(listModelEntries().toArray(Entry[]::new));
        dialog.setVisible(true);
    }

    private void hide() { dialog.setVisible(false); }

    public Entry getSelectedItem() { return list.getSelectedValue(); }

    public List<Entry> listModelEntries() {
        val list = new LinkedList<Entry>();
        list.add(Entry.of(Paths.DefaultModelFolder));
        for (val file : Paths.ModelLocationFolder.listFiles())
            if (hasValidModelStructure(file)) list.add(Entry.of(file));
        return Collections.unmodifiableList(list);
    }

    public boolean hasValidModelStructure(File file) {
        if (Objects.isNull(file)) return false;
        return checkFileExistence(new File(file, Model.CategoriesFileName))
                && checkFileExistence(new File(file, Model.FeatureExtractorParamFileName))
                && checkFileExistence(new File(file, Model.ClassifierParamFileName));
    }

    public boolean checkFileExistence(File file) {
        if (Objects.isNull(file)) return false;
        return file.exists() && file.isFile();
    }

    @Value
    @RequiredArgsConstructor(staticName = "of")
    static class Entry {
        @NonNull File file;

        public String toString() { return file.getName(); }

    }

    public static void main(String[] args) {
        val dialog = new ModelDialog(() -> List.of("cat", "dog"));
        dialog.setOnOk(e -> System.out.println("Chosen item: " + dialog.getSelectedItem()));
        dialog.show();
    }
}
