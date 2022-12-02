#!/bin/bash
read -p "NATS host :" HOST
if [ "$HOST" == "" ]; then
   exit
fi
echo "Connect to nats://$HOST" 
echo ""
read -p "Stream :"
if [ "$REPLY" == "" ]; then
   exit
fi
HOST="localhost"
echo $REPLY > channel.txt
file='channel.txt'
while read line; do
echo $line
echo `nats -s $HOST stream rm $line -f`
done < $file
rm -f $file
