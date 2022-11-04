all : Guard.class
	bash ./x.sh

Guard.class : *.java
	javac *.java

clean :
	rm -f *.class
