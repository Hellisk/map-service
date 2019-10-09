mvn compile

# sampling rate 1
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-goh -dr1" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-goh -dr1" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -dr1" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -dr1" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-eddy -dr1" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-eddy -dr1" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-FST -dr1" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-FST -dr1" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-WGT -dr1" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-WGT -dr1" -Dexec.cleanupDaemonThreads=false

# sampling rate 10
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-goh -dr10" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-goh -dr10" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -dr10" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -dr10" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-eddy -dr10" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-eddy -dr10" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-FST -dr10" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-FST -dr10" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-WGT -dr10" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-WGT -dr10" -Dexec.cleanupDaemonThreads=false

# sampling rate 20
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-goh -dr20" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-goh -dr20" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -dr20" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -dr20" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-eddy -dr20" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-eddy -dr20" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-FST -dr20" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-FST -dr20" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-WGT -dr20" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-WGT -dr20" -Dexec.cleanupDaemonThreads=false

# sampling rate 30
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-goh -dr30" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-goh -dr30" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -dr30" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -dr30" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-eddy -dr30" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-eddy -dr30" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-FST -dr30" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-FST -dr30" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-WGT -dr30" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-WGT -dr30" -Dexec.cleanupDaemonThreads=false
