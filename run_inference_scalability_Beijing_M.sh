mvn -q clean compile exec:java -Dexec.mainClass=algorithm.mapinference.MapInferenceMain -Dexec.args="-dn10000" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapinference.MapInferenceMain -Dexec.args="-dn20000" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapinference.MapInferenceMain -Dexec.args="-dn50000" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapinference.MapInferenceMain -Dexec.args="-dn-1" -Dexec.cleanupDaemonThreads=false