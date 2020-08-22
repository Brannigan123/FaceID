package hy.faceid;

import java.io.File;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Paths {
    public final File ModelLocationFolder       = new File("savedmodels");
    public final File DefaultModelFolder        = new File("model/default");
    public final File DefaultTrainingDataFolder = new File("data/training/default");
    public final File DefaultTestDataFolder     = new File("data/inference/default");

}
