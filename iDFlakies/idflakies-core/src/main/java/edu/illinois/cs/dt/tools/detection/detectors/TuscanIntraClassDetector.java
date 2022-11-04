package edu.illinois.cs.dt.tools.detection.detectors;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.io.BufferedWriter;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;

import edu.illinois.cs.dt.tools.detection.DetectionRound;
import edu.illinois.cs.dt.tools.detection.DetectorUtil;
import edu.illinois.cs.dt.tools.detection.TestShuffler;
import edu.illinois.cs.dt.tools.detection.filters.ConfirmationFilter;
import edu.illinois.cs.dt.tools.detection.filters.UniqueFilter;
import edu.illinois.cs.dt.tools.runner.InstrumentingSmartRunner;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import edu.illinois.cs.testrunner.runner.Runner;

public class TuscanIntraClassDetector extends ExecutingDetector {
    private final List<String> tests;
    private TestRunResult origResult;

    private final HashMap<String, List<String>> classToMethods;
    private final TestShuffler testShuffler;

    public static int getClassesSize(List<String> tests) {
        List<String> classes = new ArrayList<String>();        
        for (final String test : tests) {
            final String className = TestShuffler.className(test);
            if (!classes.contains(className)) {
                classes.add(className);
            }
        }
        return classes.size();
    }
    
    public static int findMaxMethodSize(HashMap<String, List<String>> classToMethods) {
        int maxMethodSize = 0;
        for (String className : classToMethods.keySet()) {
            int nn = classToMethods.get(className).size();
            if(maxMethodSize < nn) {
                maxMethodSize = nn;
            }
        }
        return maxMethodSize;
    }
    
    public TuscanIntraClassDetector(final Runner runner, final File baseDir, final int rounds, final String type, final List<String> tests) {
        super(runner, baseDir, rounds, type);
        classToMethods = new HashMap<>();
        for (final String test : tests) {
            final String className = TestShuffler.className(test);
            if (!classToMethods.containsKey(className)) {
                classToMethods.put(className, new ArrayList<>());
            }
            classToMethods.get(className).add(test);
        }
        int classSize = classToMethods.keySet().size();
        if (classSize == 3 || classSize == 5) {
            classSize++;
        }
        int maxMethodSize = findMaxMethodSize(classToMethods);
        if (maxMethodSize == 3 || maxMethodSize == 5) {
            maxMethodSize++;
        }
        this.rounds = rounds;
        if (classSize > maxMethodSize) {
            if (this.rounds > classSize) {
                this.rounds = classSize;
            }
        } else {
            if (this.rounds > maxMethodSize) {
                this.rounds = maxMethodSize;
            }
        }
        // START -- Temporary addition for experiments
	String s = Integer.toString(this.rounds);
        writeTo(baseDir + "/.dtfixingtools/num-of-orders", s);
	System.out.println("CALCULATED ROUNDS: " + this.rounds);
        this.rounds = 0;
        // END -- Temporary addition for experiments
        this.tests = tests;
        this.testShuffler = new TestShuffler(type, this.rounds, tests, baseDir);
        this.origResult = DetectorUtil.originalResults(tests, runner);
        if (runner instanceof InstrumentingSmartRunner) {
            addFilter(new ConfirmationFilter(name, tests, (InstrumentingSmartRunner) runner));
        } else {
            addFilter(new ConfirmationFilter(name, tests, InstrumentingSmartRunner.fromRunner(runner, baseDir)));
        }
        addFilter(new UniqueFilter());
    }

    public void writeTo(final String outputPath, String output) {
        if (!Files.exists(Paths.get(outputPath))) {
            try {
                Files.createFile(Paths.get(outputPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            Files.write(Paths.get(outputPath), output.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    @Override
    public DetectionRound results() throws Exception {
        return makeDts(origResult, runList(testShuffler.tuscanIntraClassOrder(absoluteRound.get())));
    }
}
