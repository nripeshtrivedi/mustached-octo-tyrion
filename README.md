# mustached-octo-tyrion

```bash
#how to install with gdal and openimaj

#note sudo can be removed if have write permissions for /usr/local 

#dependencies
sudo apt-get  update
sudo apt-get install g++ gcc
sudo apt-get install maven
sudo apt-get install swig
sudo apt-get install default-jre
sudo apt-get install default-jdk
sudo apt-get install git
sudo apt-get install  python-dev -y #only needed if --with-python is used for gdal

################################

#note this directory can be deleted once finnished

git clone https://github.com/aaceimmrttu/mustached-octo-tyrion.git ~/mustached-octo-tyrion

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
sudo cp /usr/local/src/gdal/gdal-1.11.2/swig/java/java.opt /usr/local/src/gdal/gdal-1.11.2/swig/java/java.opt.bak

sudo echo 'JAVA_HOME = "/usr/lib/jvm/default-java"  
JAVADOC=$(JAVA_HOME)/bin/javadoc 
JAVAC=$(JAVA_HOME)/bin/javac 
JAVA=$(JAVA_HOME)/bin/java 
JAR=$(JAVA_HOME)/bin/jar 
JAVA_INCLUDE=-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux' > \
/usr/local/src/gdal/gdal-1.11.2/swig/java/java.opt


sudo make
sudo make install
sudo cp gdal.jar /usr/local/lib

################################

#set up project
cd /workspace
mvn -DarchetypeCatalog=http://maven.openimaj.org/archetype-catalog.xml archetype:generate
5 #openimaj-quickstart-archetype
edu.wright.wacs #project package
wamiNet #project name

#continue until finnished


cd wamiNet
mvn assembly:assembly
#test openimaj
java -jar target/wamiNet-1.0-SNAPSHOT-jar-with-dependencies.jar

################################

#port existing code
cp -r ~/* ~/workspace/wamiNet/
git init
git remote add origin https://github.com/aaceimmrttu/mustached-octo-tyrion.git
git fetch
git checkout -t origin/master


################################

#set up eclipse
cd ~/workspace/wamiNet
mvn eclipse:eclipse
#open eclipse and import existing project

#add project variables
# M2_REPO = ~/.m2/repository/
# LD_LIBRARY_PATH = /usr/local/lib/
#add gdal.jar to classpath with its native library = /usr/local/lib

################################

#final note, there may be a better way to do this, but I know this way works :-)
