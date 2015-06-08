# mustached-octo-tyrion

```bash
#how to install gdal with java, maven, and openimaj

#note sudo can be removed if have permissions for /usr/local 

#update
sudo apt-get install maven
sudo apt-get install swig
sudo apt-get  update; sudo apt-get install  python-dev -y
sudo apt-get install default-jre
sudo apt-get install default-jdk

################################

#download proj4
sudo wget http://download.osgeo.org/proj/proj-4.9.1.tar.gz
sudo tar xvfz proj-4.9.1.tar.gz 
cd proj-4.9.1/
sudo ./configure 
sudo make
sudo make install

################################

#download gdal
sudo wget http://download.osgeo.org/gdal/1.11.2/gdal-1.11.2.tar.gz
sudo tar -xzvf gdal-1.11.2.tar.gz 
cd gdal-1.11.2/
sudo ./configure --with-python --with-jpeg --with-java --with-png --with-static-proj4=/usr/local/lib
sudo make
sudo make install

#modify java.opt to use the correct jvm path
sudo echo "JAVA_HOME = "/usr/lib/jvm/default-java"  \
JAVADOC=$(JAVA_HOME)/bin/javadoc \
JAVAC=$(JAVA_HOME)/bin/javac \
JAVA=$(JAVA_HOME)/bin/java \
JAR=$(JAVA_HOME)/bin/jar \
JAVA_INCLUDE=-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux" > \
/usr/local/src/gdal/gdal-1.11.2/swig/java/java.opt


sudo make
sudo make install

################################

#test openimaj
cd /workspace
mvn -DarchetypeCatalog=http://maven.openimaj.org/archetype-catalog.xml archetype:generate
5 #openimaj-quickstart-archetype
edu.wright.wacs
wamiNet

#continue until finnished


cd wamiNet
mvn assembly:assembly
java -jar target/OpenIMAJ-Tutorial01-1.0-SNAPSHOT-jar-with-dependencies.jar

#set up eclipse
mvn eclipse:eclipse

################################
