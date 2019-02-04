#!/bin/bash

cd /root/cordaLoyalty/kotlin-source/build/nodes
cd Notary
java -jar corda.jar &
sleep 60
cd ..


cd Eni
java -jar corda.jar &
sleep 2
java -jar corda-webserver.jar &
sleep 60
cd ..


cd Douglas
java -jar corda.jar &
sleep 2
java -jar corda-webserver.jar &
sleep 60
cd ..


cd ePrice
java -jar corda.jar &
sleep 2
java -jar corda-webserver.jar &
sleep 60
cd ..


cd nordFood
java -jar corda.jar &
sleep 2
java -jar corda-webserver.jar &
sleep infinity