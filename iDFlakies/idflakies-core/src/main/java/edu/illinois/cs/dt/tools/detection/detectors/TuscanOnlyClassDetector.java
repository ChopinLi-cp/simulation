package edu.illinois.cs.dt.tools.detection.detectors;

import java.io.File;
import java.util.ArrayList;
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
import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import edu.illinois.cs.testrunner.runner.Runner;
import edu.illinois.starts.helpers.Writer;

public class TuscanOnlyClassDetector extends ExecutingDetector {
    private final List<String> tests;
    private TestRunResult origResult;

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
    
    public TuscanOnlyClassDetector(final Runner runner, final File baseDir, final int rounds, final String type, final List<String> tests) {
        super(runner, baseDir, rounds, type);
        int n = getClassesSize(tests);
        if (n == 3 || n == 5) {
            // We need one more round than the number of classes if n is 3 or 5.
            if (this.rounds > n) {
                this.rounds = n + 1;
            }
        } else {
            if (this.rounds > n) {
                this.rounds = n;
            }
        }
        this.tests = tests;
        // START -- Temporary addition for experiments
        int num_of_order = this.rounds;
        int tmp_num = num_of_order;
				String s = Integer.toString(this.rounds);
        writeTo(baseDir + "/.dtfixingtools/num-of-orders", s);
	    	System.out.println("CALCULATED ROUNDS: " + this.rounds);
	    	this.rounds = 0;
        // END -- Temporary addition for experiments
        this.testShuffler = new TestShuffler(type, this.rounds, tests, baseDir);
        this.origResult = DetectorUtil.originalResults(tests, runner);
        if (runner instanceof InstrumentingSmartRunner) {
            addFilter(new ConfirmationFilter(name, tests, (InstrumentingSmartRunner) runner));
        } else {
            addFilter(new ConfirmationFilter(name, tests, InstrumentingSmartRunner.fromRunner(runner, baseDir)));
        }
        addFilter(new UniqueFilter());
        num_of_order = Integer.parseInt(Configuration.config().getProperty("dt.detector.rounds.endIndex", num_of_order + ""));
        if (num_of_order > tmp_num) {
						num_of_order = tmp_num;
				}
				int i = Integer.parseInt(Configuration.config().getProperty("dt.detector.rounds.startIndex", "0"));;
        for (; i < num_of_order; i ++) {
            List<String> order = testShuffler.alphabeticalAndTuscanOrder(i, true);
            writeOrder(order, baseDir + "/.dtfixingtools", i);
        }
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

    private void writeOrder(List<String> order, String artifactsDir, int index) {
        String outFilename = Paths.get(artifactsDir + "/orders", "order-" + index).toString();
        try (BufferedWriter writer = Writer.getWriter(outFilename)) {
            for (String test : order) {
                int i = this.tests.indexOf(test);
                String s = Integer.toString(i);
                writer.write(s);
                writer.write(System.lineSeparator());
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    @Override
    public DetectionRound results() throws Exception {
        return makeDts(origResult, runList(testShuffler.alphabeticalAndTuscanOrder(absoluteRound.get(), true)));
    }
}
