mvn -q clean compile exec:java -Dexec.mainClass=algorithm.mapinference.MapInferenceMain -Dexec.args="-dn50000" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapinference.MapInferenceMain -Dexec.args="-dn100000" -Dexec.cleanupDaemonThreads=false