all:
	javac -classpath ~/tdroid/tdroid-4.1.1_r6/frameworks/base/services/java/ PConstraint.java
	javac -classpath ~/tdroid/tdroid-4.1.1_r6/frameworks/base/services/java/ TMLogcatTester.java
	javac -classpath ~/tdroid/tdroid-4.1.1_r6/frameworks/base/services/java/ TMLogcat.java

test:
	java -classpath ~/tdroid/tdroid-4.1.1_r6/frameworks/base/services/java/ com.android.server.tmservice.TMLogcatTester log.txt

clean:
	rm -f *.class