package hy.faceid.ui;

import static hy.API.L2Regularizer;
import static hy.API.NArray;
import static hy.API.SoftmaxCrossEntropy;
import static hy.API.TrainConfig;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

import hy.faceid.Paths;
import hy.faceid.graph.GraphPanel;
import hy.faceid.graph.Point;
import hy.faceid.model.Model;
import hy.model.Model.BatchLossCallback;
import hy.model.Model.EpochLossCallback;
import hy.model.Model.TrainConfig;
import hy.util.Format;
import hy.util.Image;
import hy.util.NArray;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TrainingPanel extends JPanel {

    static final Duration saveIntervalThreshold = Duration.ofMinutes(3);

    String                dataFolder            = Paths.DefaultTrainingDataFolder.getAbsolutePath();
    String                modelFolder           = Paths.DefaultModelFolder.getAbsolutePath();

    Model                 model                 = Model.from(modelFolder);

    Thread                trainingThread;

    @SuppressWarnings("deprecation")
    public TrainingPanel(@NonNull Consumer<String> titleSuffix) {
        setLayout(new BorderLayout(4, 4));

        val menuBar = new JMenuBar();
        val modelFolderPickerButton = new JButton("Model");
        val modelFolderChooser = new ModelDialog(() -> List.of(new File(dataFolder).list()));
        val dataFolderPickerButton = new JButton("Select Data");
        val dataFolderChooser = new JFileChooser();
        val startStopTrainingButton = new JButton("Start Training");

        val graphPanel = new JSplitPane();
        val epochLossGraph = new GraphPanel("epoch", "loss", 18, 70, 400);
        val batchLossGraph = new GraphPanel("batch", "loss", 18, 70, 400);

        val loggingPanel = new JPanel();
        val epochLabel = new JLabel();
        val epochText = new JTextArea();
        val errorLabel = new JLabel();
        val errorText = new JTextArea();
        val logText = new JTextArea();

        menuBar.setMargin(new Insets(4, 4, 4, 4));
        menuBar.add(modelFolderPickerButton);
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(dataFolderPickerButton);
        menuBar.add(Box.createHorizontalStrut(8));
        menuBar.add(startStopTrainingButton);

        dataFolderPickerButton.setToolTipText("Open training data folder");
        modelFolderPickerButton.setToolTipText("Open model folder");

        dataFolderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        dataFolderChooser.setMultiSelectionEnabled(false);

        startStopTrainingButton.setToolTipText("Start training model");

        graphPanel.setResizeWeight(0.5);
        graphPanel.setBackground(new Color(67, 73, 74));
        graphPanel.setLeftComponent(epochLossGraph);
        graphPanel.setRightComponent(batchLossGraph);

        epochLossGraph.setCapacity(100);
        batchLossGraph.setCapacity(100);

        loggingPanel.add(epochLabel);
        loggingPanel.add(epochText);
        loggingPanel.add(Box.createHorizontalGlue());
        loggingPanel.add(errorLabel);
        loggingPanel.add(errorText);
        loggingPanel.add(Box.createHorizontalGlue());
        loggingPanel.add(logText);

        epochLabel.setText("Epoch");
        epochText.setText("0");
        epochText.setEnabled(false);
        epochText.setBackground(new Color(67, 73, 74));

        errorLabel.setText("Loss");
        errorText.setText("0");
        errorText.setEnabled(false);
        errorText.setBackground(new Color(67, 73, 74));

        logText.setEnabled(false);
        logText.setBackground(new Color(67, 73, 74));
        logText.setFont(new Font("Consolas", Font.PLAIN, 12));
        logText.setText("Not Training");

        add(menuBar, BorderLayout.NORTH);
        add(graphPanel, BorderLayout.CENTER);
        add(loggingPanel, BorderLayout.SOUTH);

        dataFolderPickerButton.addActionListener(e -> {
            dataFolderChooser.setCurrentDirectory(new File(dataFolder));
            if (dataFolderChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                dataFolder = dataFolderChooser.getSelectedFile();
            }
        });

        startStopTrainingButton.addActionListener(e -> {
            if (Objects.nonNull(trainingThread) && trainingThread.isAlive()) {
                trainingThread.stop();
                trainingThread = null;
                startStopTrainingButton.setText("Start Training");
                logText.setText("Training Terminated");
                return;
            }
            val response = JOptionPane.showInputDialog("Please enter number of training epochs.\nDefaults to 1000",
                1000L);
            val epochs = Try.of(() -> Long.parseLong(response)).getOrElse(1000L);
            train(epochs, startStopTrainingButton, epochText, errorText, logText, epochLossGraph, batchLossGraph);
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
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(null, "Failed to load model :\n" + exception);
            }
        });

    }

    public String getModelName() { return modelFolder.substring(modelFolder.lastIndexOf(File.separatorChar) + 1); }

    private void train(Long epochs, JButton startStopTrainingButton, JTextArea epochText, JTextArea errorText,
        JTextArea logText, GraphPanel epochLossGraph, GraphPanel batchLossGraph) {
        trainingThread = new Thread(() -> {
            startStopTrainingButton.setText("Stop Training");
            logText.setText("Loading data");
            val x = new ArrayList<NArray>();
            val y = new ArrayList<NArray>();
            loadData(x, y);
            logText.setText("Loaded data");

            if (x.size() > 0 && y.size() > 0) {
                logText.setText("Configuring");
                val lastSavedInstant = new AtomicReference<>(Instant.now());
                val trainConfig = generateTrainConfig(epochs, epochText, errorText, logText, epochLossGraph,
                    batchLossGraph, x, y, lastSavedInstant);
                logText.setText("Configured");

                logText.setText("Initiating Training");
                batchLossGraph.clear();
                model.fit(trainConfig);

                logText.setText("Saving model");
                model.save(modelFolder);
                logText.setText("Saved model");
            } else {
                JOptionPane.showMessageDialog(null, "Training data not available");
            }
            startStopTrainingButton.setText("Start Training");
            logText.setText("Training Terminated");
        });
        trainingThread.start();
    }

    private TrainConfig generateTrainConfig(Long epochs, JTextArea epochText, JTextArea errorText, JTextArea logText,
        GraphPanel epochLossGraph, GraphPanel batchLossGraph, ArrayList<NArray> x, ArrayList<NArray> y,
        AtomicReference<Instant> lastSavedInstant) {
        return TrainConfig().inputs(x).targets(y).batchSize(20).shuffle(true) //
                .loss(SoftmaxCrossEntropy).regularizer(L2Regularizer(0.006)) //
                .epochs(Math.abs(epochs)) //
                .epochLossCallBack(epochLossCallBack(epochText, errorText, epochLossGraph))//
                .batchLossCallback(batchLossCallback(logText, lastSavedInstant, batchLossGraph))//
                .build();
    }

    private void loadData(List<NArray> x, List<NArray> y) {
        try {
            val categories = model.getCategories();
            val n = categories.size();
            for (int i = 0; i < n; i++) {
                val label = NArray(n)[i] = 1;
                val catpath = new File(dataFolder, categories[i]);
                readData(Image.paths(catpath.getAbsolutePath()), label, x, y);
            }
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(null, "Failed to load data :\n" + exception);
        }
    }

    private void readData(String[] paths, NArray label, List<NArray> x, List<NArray> y) {
        for (val imagPath : paths) {
            x.add(Image.read(imagPath, 256, 256));
            y.add(label);
        }
    }

    private BatchLossCallback batchLossCallback(JTextArea logText, AtomicReference<Instant> lastSavedInstant,
        GraphPanel graph) {
        return (batch, count, size, lossType, loss, averageLoss) -> {
            logText.setText(BatchLossCallback.format(20, batch, count, size, lossType, loss, averageLoss));
            if (batch == 1 && count == 1) graph.clear();
            graph.add("Batch loss", Point.at(Color.lightGray, batch + count / (double) size, averageLoss));
            val now = Instant.now();
            if (Duration.between(lastSavedInstant.get(), now).compareTo(saveIntervalThreshold) >= 0) {
                logText.setText("Saving model");
                model.save();
                lastSavedInstant.set(now);
                logText.setText("Saved model");
            }
        };
    }

    private @NonNull EpochLossCallback epochLossCallBack(JTextArea epochText, JTextArea errorText, GraphPanel graph) {
        return (epoch, totalEpochs, lossType, loss) -> {
            epochText.setText(Format.fmtFraction(epoch, totalEpochs));
            errorText.setText(lossType + ": " + Format.fmtDecimal(loss));
            graph.add("Epoch Loss", Point.at(Color.lightGray, epoch, loss));
        };
    }

}
