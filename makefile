

default: 
	javac -g -cp /lusr/share/opt/junit-4.5/junit-4.5.jar *.java

clean:
	$(RM) *.class
