FILES = \
ADisk.java \
ADiskUnit.java \
BitMap.java \
Common.java \
DirEnt.java \
DiskCallback.java \
Disk.java \
DiskResult.java \
DiskUnit.java \
DiskWorker.java \
FlatFS.java \
FlatFSUnit.java \
Helper.java \
InternalNode.java \
Node.java \
PTree.java \
PTreeUnit.java \
ResourceException.java \
RFS.java \
Sector.java \
SimpleLock.java \
TNode.java \
Transaction.java \
TransID.java

junit_loc = /lusr/share/opt/junit-4.5.jar

# Set up the classpaths for Javac
classpath:= \
        $(junit_loc)

# Convert the spaces to colons.  This trick is from 
# the make info file.
empty:=
space:= $(empty) $(empty)
classpath:=     $(subst $(space),:,$(classpath))

# If there's already a CLASSPATH, put it on the front
ifneq ($(CLASSPATH),)
        classpath:=     $(CLASSPATH):$(classpath)
endif

# Re-export the CLASSPATH.
export CLASSPATH:=$(classpath)

default $(FILES): 
	@echo "Making"
	@javac $(FILES)
	
unit: default
	@echo "Starting Tests"
	java org.junit.runner.JUnitCore ADiskUnit 
	java org.junit.runner.JUnitCore PTreeUnit 
	java org.junit.runner.JUnitCore FlatFSUnit

clean:
	@echo "Cleaning"
	@rm -f *.class
