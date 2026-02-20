#!/bin/bash

echo $CLASSPATH
for p in $(ls ./lib/*.jar); do
    echo $p
    echo `ls -l $p`
done

