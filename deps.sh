#!/usr/bin/env bash
mkdir tmp
wget -c --output-document tmp/netcdfAll-4.3.jar ftp://ftp.unidata.ucar.edu/pub/netcdf-java/v4.3/netcdfAll-4.3.jar
mvn install:install-file -Dfile=tmp/netcdfAll-4.3.jar -DgroupId=edu.ucar -DartifactId=netcdf -Dversion=4.3.2 -Dpackaging=jar -DgeneratePom=true
