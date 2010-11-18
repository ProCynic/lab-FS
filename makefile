

default: 
	@echo "Making"
	@javac *.java
	
unit:
	@java org.junit.runner.JUnitCore ADiskUnit

clean:
	@echo "Cleaning"
	@rm -f *.class
