package hy.faceid.model;

import static hy.API.Conv;
import static hy.API.FC;
import static hy.API.Gauss;
import static hy.API.MaxPool;
import static hy.API.Sequential;
import static hy.API.Softmax;
import static hy.API.Tanh;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import javax.swing.JOptionPane;

import hy.model.Model.TrainConfig;
import hy.faceid.Paths;
import hy.model.Sequential;
import hy.util.Image;
import hy.util.NArray;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.experimental.FieldDefaults;

@FieldDefaults(makeFinal = true)
public class Model {
    public static final String CategoriesFileName            = "categories.dat";
    public static final String FeatureExtractorParamFileName = "feature extractor.pm";
    public static final String ClassifierParamFileName       = "classifier.pm";

    String                     path;
    String[]                   categories;

    Sequential                 fe;
    Sequential                 fc;
    Sequential                 cnn;

    @SneakyThrows
    public Model(@NonNull String path) {
        val categoriesScanner = new Scanner(new File(path, CategoriesFileName));
        val featureExtrcorParamsFile = new File(path, FeatureExtractorParamFileName);
        val classifierParamFile = new File(path, ClassifierParamFileName);
        this.path = path;
        this.categories = categoriesScanner.tokens().toArray(String[]::new);
        this.fe = buildFeatureExtractor(featureExtrcorParamsFile.getAbsolutePath());
        this.fc = buildClassifier(categories.length, classifierParamFile.getAbsolutePath());
        this.cnn = Sequential().add(fe).add(fc);
    }

    @SneakyThrows
    public Model(@NonNull String path, @NonNull Sequential fe, @NonNull Sequential fc) {
        val categoriesScanner = new Scanner(new File(path, CategoriesFileName));
        this.path = path;
        this.categories = categoriesScanner.tokens().toArray(String[]::new);
        this.fe = fe;
        this.fc = fc;
        this.cnn = Sequential().add(fe).add(fc);
    }

    public static Model from(String path) { return new Model(path); }

    public static boolean createNewModel(String name, @NonNull List<String> categories) {
        try {
            if (!tryCreatingFolder(name)) return false;
            val path = new File(Paths.ModelLocationFolder, name).getAbsolutePath();
            Files.write(Path.of(path, CategoriesFileName), categories);
            val fe = buildFeatureExtractor(
                new File(Paths.DefaultModelFolder, FeatureExtractorParamFileName).getAbsolutePath());
            val fc = buildClassifier(categories.size(),
                new File(Paths.DefaultModelFolder, ClassifierParamFileName).getAbsolutePath());
            val model = new Model(path, fe, fc);
            model.save();
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Failed to create model '" + name + "'.\n" + e.getMessage(),
                "Model creation failed", JOptionPane.WARNING_MESSAGE);
            return false;
        }
    }

    public static boolean tryCreatingFolder(@NonNull String name) {
        val newModelFile = new File(Paths.ModelLocationFolder, name);
        if (newModelFile.exists()) {
            return JOptionPane.showConfirmDialog(null, "Entry '" + name + "' already exists.\n Replace existing files?",
                "Replace Model", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
        }
        newModelFile.mkdirs();
        return true;
    }

    private static Sequential buildFeatureExtractor(String featureExtrcorParamsPath) {
        return Sequential() // 3*256*256
                .add(Conv(4, 3, 3)) // 4*254*254
                .add(Conv(5, 4, 3)) // 5*252*252
                .add(MaxPool(2), Gauss) // 5*126*126
                .dropout(0.3) // 5*126*126
                .add(Conv(6, 5, 3)) // 6*124*124
                .add(Conv(6, 6, 3)) // 6*122*122
                .add(MaxPool(2), Gauss) // 6*61*61
                .dropout(0.3) // 6*61*61
                .add(Conv(5, 6, 3)) // 5*59*59
                .add(Conv(5, 5, 3)) // 5*57*57
                .add(MaxPool(3), Gauss) // 5*19*19
                .load(featureExtrcorParamsPath);
    }

    private static Sequential buildClassifier(int numOfClasses, String classifierParamPath) {
        return Sequential() // 5*19*19
                .flatten() // 1805
                .add(FC(1805, 1000), Tanh) // 1000
                .dropout(0.3) // 1000
                .add(FC(1000, 500), Tanh) // 500
                .dropout(0.2) // 500
                .add(FC(500, 50), Tanh) // 50
                .add(FC(50, 20), Tanh) // 20
                .add(FC(20, numOfClasses), Softmax) // # classes
                .load(classifierParamPath);
    }

    public void load(String path) {
        val featureExtrcorParamsPath = new File(path, "feature extractor.pm").getAbsolutePath();
        val classifierParamPath = new File(path, "classifier.pm").getAbsolutePath();
        fe.load(featureExtrcorParamsPath);
        fc.load(classifierParamPath);
    }

    public void save(String path) {
        val featureExtrcorParamsPath = new File(path, "feature extractor.pm").getAbsolutePath();
        val classifierParamPath = new File(path, "classifier.pm").getAbsolutePath();
        fe.save(featureExtrcorParamsPath);
        fc.save(classifierParamPath);
    }

    public Model save() {
        save(path);
        return this;
    }

    public List<String> getCategories() { return List.of(categories); }

    public String infer(String path) { return label(cnn.predict(Image.read(path, 256, 256))); }

    private String label(NArray logits) {
        return Array.ofAll(logits).zipWithIndex()//
                .<Double>maxBy(Tuple2::_1).<String>map(pi -> categories[pi._2]).get();
    }

    public Model fit(TrainConfig config) {
        cnn.fit(config);
        return this;
    }

}
