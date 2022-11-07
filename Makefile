all : Guard.class
	#bash ./x.sh
	java -ea Main

Guard.class : *.java
	javac *.java

clean :
	rm -f *.class
