mvn compile
# test on-hmm-fixed on all maps
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -ddBeijing-S" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -ddBeijing-S" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -ddBeijing-L" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -ddBeijing-L" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -ddBeijing-R" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -ddBeijing-R" -Dexec.cleanupDaemonThreads=false

# test on-wgt on all maps
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-WGT -ddBeijing-S" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-WGT -ddBeijing-S" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-WGT -ddBeijing-L" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-WGT -ddBeijing-L" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-WGT -ddBeijing-R" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-WGT -ddBeijing-R" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-WGT -ddBeijing-M" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-WGT -ddBeijing-M" -Dexec.cleanupDaemonThreads=false

# test on-fst on all maps
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-FST -ddBeijing-S" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-FST -ddBeijing-S" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-FST -ddBeijing-L" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-FST -ddBeijing-L" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-FST -ddBeijing-R" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-FST -ddBeijing-R" -Dexec.cleanupDaemonThreads=false
