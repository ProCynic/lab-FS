

default: 
	@echo "Making"
	@javac *.java
	
unit:
	@echo "BANG!"

clean:
	@echo "Cleaning"
	@rm -f *.class
