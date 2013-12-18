JAVAC=/usr/bin/javac
JAVA=/usr/bin/java

CLASSPATH=~/tdroid/tdroid-4.1.1_r6/frameworks/base/services/java/

all:
	$(JAVAC) -classpath $(CLASSPATH) PConstraint.java
	$(JAVAC) -classpath $(CLASSPATH) TMLocationAnalyzer.java
	$(JAVAC) -classpath $(CLASSPATH) TMLogcat.java

test: log0.txt log1.txt log2.txt all
	$(JAVA) -classpath $(CLASSPATH) com.android.server.tmservice.TMLocationAnalyzer log0.txt
	$(JAVA) -classpath $(CLASSPATH) com.android.server.tmservice.TMLocationAnalyzer log1.txt
	$(JAVA) -classpath $(CLASSPATH) com.android.server.tmservice.TMLocationAnalyzer log2.txt

clean:
	rm -f *.class