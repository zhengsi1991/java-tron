#!/usr/bin/env bash
i=1
costnumber=0
cat  logs/tron.log | grep "cost/txs"  | while read line2
do
cost=`echo $line2 | awk -F ':' '{print $6}' | awk -F '/' '{print $1}'`
let costnumber=cost+costnumber
let i=i+1
echo $costnumber" number"
echo $i "number2"
done
let result=costnumber/i
echo  $costnumber "wefwef"
echo $i "ddd"
echo $result "bbbb"