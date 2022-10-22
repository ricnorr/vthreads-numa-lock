package ru.ricnorr.locks.numa.jmh;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.openjdk.jmh.profile.LinuxPerfNormProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class RunBenchmarks {
    public static void main(String[] args) throws RunnerException, ParseException {
        org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();
        options.addOption("include", true, "include benchmarks");
        options.addOption("threads", true, "threads for benchmarks");
        options.addOption("lockType", true, "lock type");
        options.addOption("forks", true, "forks");
        options.addOption("perfnorm", true, "use perf norm profiler");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        OptionsBuilder optionsBuilder = new OptionsBuilder();

        String includeOptions = cmd.getOptionValue("include", ".*Benchmark");
        for (String x : includeOptions.split(",")) {
            optionsBuilder.include(x);
        }

        String lockTypes = cmd.getOptionValue("lockType", Stream.of(LockType.values()).map(LockType::name).collect(
            Collectors.joining(",")));
        optionsBuilder.param("lockType", lockTypes.split(","));

        String threadsOption = cmd.getOptionValue("threads", "16");
        List<Integer> threadsList =
            Arrays.stream(threadsOption.split(",")).map(Integer::valueOf).collect(Collectors.toList());

        String forkStr = cmd.getOptionValue("forks", "5");
        int forkCnt = Integer.parseInt(forkStr);
        optionsBuilder.forks(forkCnt);

        boolean usePerfNormProfiler = Boolean.parseBoolean(cmd.getOptionValue("perf-norm", "false"));
        if (usePerfNormProfiler) {
            optionsBuilder.addProfiler(LinuxPerfNormProfiler.class); // have useful stat "LLC-load-misses"
        }
        for (int threadCnt : threadsList) {
            Options benchmarkOptions = optionsBuilder
                .threads(threadCnt)
                .build();
            new Runner(benchmarkOptions).run();
        }
    }
}
