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
cat <<EOF > rep.cfg
 {
    "subjects": [
      "$line.\u003e"
    ],
    "retention": "limits",
    "max_consumers": -1,
    "max_msgs_per_subject": -1,
    "max_msgs": -1,
    "max_bytes": -1,
    "max_age": 86400000000000,
    "max_msg_size": 8388608,
    "storage": "file",
    "discard": "old",
    "num_replicas": 3,
    "duplicate_window": 120000000000,
    "sealed": false,
    "deny_delete": false,
    "deny_purge": false,
    "allow_rollup_hdrs": false
}
EOF
echo `nats -s $HOST stream rm $line -f`
echo `nats -s $HOST stream add $line --config=rep.cfg -j`
done < $file
rm -f $file
rm -f 'rep.cfg'
