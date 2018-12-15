#!/bin/bash
i=1
cat tmp.txt | while read line2
do
curl -X POST http://47.89.177.99:8091/walletsolidity/gettransactionbyid -d '{ "value":"'$line2'"}' > ansx$i.txt

curl -X POST http://47.90.248.162:8091/walletsolidity/gettransactionbyid -d '{"value":"'$line2'"}' > ans$i.txt

diff ansx$i.txt ans$i.txt >> diffcontent
let i=i+1;
done
