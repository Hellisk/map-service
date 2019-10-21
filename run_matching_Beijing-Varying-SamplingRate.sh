mvn compile

# sampling rate 1
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-goh -sa1" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-goh -sa1" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -sa1" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -sa1" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-eddy -sa1" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-eddy -sa1" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-FST -sa1" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-FST -sa1" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-WGT -sa1" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-WGT -sa1" -Dexec.cleanupDaemonThreads=false

# sampling rate 5
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-goh -sa5" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-goh -sa5" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -sa5" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -sa5" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-eddy -sa5" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-eddy -sa5" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-FST -sa5" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-FST -sa5" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-WGT -sa5" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-WGT -sa5" -Dexec.cleanupDaemonThreads=false


# sampling rate 10
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-goh -sa10" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-goh -sa10" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -sa10" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -sa10" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-eddy -sa10" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-eddy -sa10" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-FST -sa10" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-FST -sa10" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-WGT -sa10" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-WGT -sa10" -Dexec.cleanupDaemonThreads=false

# sampling rate 20
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-goh -sa20" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-goh -sa20" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -sa20" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -sa20" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-eddy -sa20" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-eddy -sa20" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-FST -sa20" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-FST -sa20" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-WGT -sa20" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-WGT -sa20" -Dexec.cleanupDaemonThreads=false

# sampling rate 30
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-goh -sa30" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-goh -sa30" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-fixed -sa30" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-fixed -sa30" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-HMM-eddy -sa30" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-HMM-eddy -sa30" -Dexec.cleanupDaemonThreads=false
#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-FST -sa30" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-FST -sa30" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-WGT -sa30" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-WGT -sa30" -Dexec.cleanupDaemonThreads=false

#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-FST -sa50" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-FST -sa50" -Dexec.cleanupDaemonThreads=false

mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-WGT -sa50" -Dexec.cleanupDaemonThreads=false
mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-WGT -sa50" -Dexec.cleanupDaemonThreads=false

#mvn exec:java -Dexec.mainClass=algorithm.mapmatching.MapMatchingMain -Dexec.args="-mmON-WGT -sa120" -Dexec.cleanupDaemonThreads=false
#mvn compile exec:java -Dexec.mainClass=evaluation.matchingevaluation.MapMatchingEvaluationMain -Dexec.args="-mmON-WGT -sa120" -Dexec.cleanupDaemonThreads=false
