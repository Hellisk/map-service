mvn -q clean compile exec:java -Dexec.mainClass=algorithm.mapinference.MapInferenceMain -Dexec.args="-dn1000" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapinference.MapInferenceMain -Dexec.args="-dn2500" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapinference.MapInferenceMain -Dexec.args="-dn5000" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapinference.MapInferenceMain -Dexec.args="-dn10000" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapinference.MapInferenceMain -Dexec.args="-dn15000" -Dexec.cleanupDaemonThreads=false
mvn exec:java -Dexec.mainClass=algorithm.mapinference.MapInferenceMain -Dexec.args="-dn-1" -Dexec.cleanupDaemonThreads=false