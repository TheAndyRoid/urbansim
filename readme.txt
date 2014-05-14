To run this project requires the following:
Linux x64 (Tested with Ubuntu)
Java 7 from Oracle
Working instalation of SUMO Library (simulation of urban mobility)sudo apt-get install sumo sumo-tools sumo-doc)

To run this project cd to the root project directory and run.
bash ./run.sh -file ./test/control/case.xml

To complie this project requires the following:
Linux x64 (Tested with Ubuntu)
Java 7 from Oracle
Working instalation of SUMO Library (simulation of urban mobility)
Eclipse IDE

To compile:
Import the project into Eclipse.
Import the user Libraries.
Build only the Simulator project in Eclipse.
Import the launch configuration into Eclipse 
Run the project from Eclipse using the launch configuration.


Folders:
jar    : Contains the compiled Java classes,
lib    : Libraries that are used in this project that are uncommon,small or require lots of work to get required files.
src    : Contains source files for this project
test   : Contains Configuration files that are used to test the simulator.
tools  : Contains a simple tool to convert OSM busstops to static devices.

Files         
run.sh             : Used to run the simulator.
runConvert.sh      : Used to run the OSM to bus stop to static device conversion tool.
Console.launch     : Run configuration for Eclipse.
libs.userlibraries : User libraries to import into Eclipse


