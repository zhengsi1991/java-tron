#!/bin/bash
cat iplist | while read line2
do
cat 1.txt | grep -i $line2" miss a block"  >> diffcontent

done
