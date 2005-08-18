#!/bin/sh

DIR=m2

rm -rf $DIR > /dev/null 2>&1
mkdir $DIR

cp -r src $DIR
cp pom.xml $DIR

(
  cd $DIR

  find . -name '\.svn' -exec rm -rf {} \;

  (
    cd src

    mkdir main ; mv java main 
  
    ( cd test ; mkdir java ; mv org java )
  
  )

)
