mvn compile
# test on-wgt on all maps
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-WGT -do2" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-WGT -do2" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-WGT -do4" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-WGT -do4" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-WGT -do8" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-WGT -do8" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-WGT -do12" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-WGT -do12" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-WGT -do16" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-WGT -do16" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-WGT -do20" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-WGT -do20" -Dexec.cleanupDaemonThreads=false


# test on-hmm-fixed on all maps
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -do2" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -do2" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -do4" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -do4" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -do8" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -do8" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -do12" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -do12" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -do16" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -do16" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -do20" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -do20" -Dexec.cleanupDaemonThreads=false

# test on-fst on all maps
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-FST -do2" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-FST -do2" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-FST -do4" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-FST -do4" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-FST -do8" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-FST -do8" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-FST -do12" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-FST -do12" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-FST -do16" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-FST -do16" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-FST -do20" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-FST -do20" -Dexec.cleanupDaemonThreads=false
