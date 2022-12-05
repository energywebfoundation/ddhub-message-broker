#!/bin/bash
read -p "NATS host :" HOST
if [ "$HOST" == "" ]; then
   exit
fi
echo "Connect to nats://$HOST" 
echo ""
read -p "Are you sure to continue?"
if [ "$REPLY" != "yes" ]; then
   exit
fi

echo `nats -s $HOST stream ls -n > channel.txt`
file='channel.txt'
while read line; do
echo $line
echo `nats -s $HOST stream rm $line -f`
done < $file
rm -f $file
