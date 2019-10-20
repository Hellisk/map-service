mvn compile
# experiments on goh
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-goh -wt1" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-goh -wt1" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-goh -wt10" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-goh -wt10" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-goh -wt20" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-goh -wt20" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-goh -wt35" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-goh -wt35" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-goh -wt50" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-goh -wt50" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-goh -wt100" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-goh -wt100" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-goh -wt200" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-goh -wt200" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-goh -wt300" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-goh -wt300" -Dexec.cleanupDaemonThreads=false

# experiment on fixed (on & off)
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmOF-HMM" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmOF-HMM" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -wt1" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -wt1" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -wt10" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -wt10" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -wt20" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -wt20" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -wt35" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -wt35" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -wt50" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -wt50" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -wt100" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -wt100" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -wt200" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -wt200" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -wt300" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -wt300" -Dexec.cleanupDaemonThreads=false

# experiment on eddy
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-eddy -ga0.000001" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-eddy -ga0.000001" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-eddy -ga0.0001" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-eddy -ga0.0001" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-eddy -ga0.001" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-eddy -ga0.001" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-eddy -ga0.01" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-eddy -ga0.01" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-eddy -ga0.1" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-eddy -ga0.1" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-eddy -ga1" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-eddy -ga1" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-eddy -ga2" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-eddy -ga2" -Dexec.cleanupDaemonThreads=false

# experiment on on-fst
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-FST -wt1" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-FST -wt1" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-FST -wt20" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-FST -wt20" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-FST -wt35" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-FST -wt35" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-FST -wt50" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-FST -wt50" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-FST -wt100" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-FST -wt100" -Dexec.cleanupDaemonThreads=false

#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmOF-HMM -ms0.1" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmOF-HMM -ms0.1" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmOF-HMM -ms0.25" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmOF-HMM -ms0.25" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmOF-HMM -ms0.5" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmOF-HMM -ms0.5" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmOF-HMM -ms0.75" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmOF-HMM -ms0.75" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmOF-HMM -ms0.05" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmOF-HMM -ms0.05" -Dexec.cleanupDaemonThreads=false

#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmOF-HMM -mb0.2" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmOF-HMM -mb.02" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmOF-HMM -mb0.5" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmOF-HMM -mb0.5" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmOF-HMM -mb1" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmOF-HMM -mb1" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmOF-HMM -mb3" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmOF-HMM -mb3" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmOF-HMM -mb5" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmOF-HMM -mb5" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmOF-HMM -mb7" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmOF-HMM -mb7" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmOF-HMM -mb10" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmOF-HMM -mb10" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmOF-HMM -mb13"-DexeccleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmOF-HMM -mb13" -Dexec.cleanupDaemonThreads=false


# test on beta
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -mb0.1" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -mb0.1" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -mb0.2" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -mb0.2" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -mb0.3" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -mb0.3" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -mb0.4" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -mb0.4" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -mb1.2" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -mb1.2" -Dexec.cleanupDaemonThreads=false

#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -wt1" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -wt1" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmOF-HMM-old" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmOF-HMM-old" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmOF-HMM" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmOF-HMM" -Dexec.cleanupDaemonThreads=false
