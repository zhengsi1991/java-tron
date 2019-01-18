#!/bin/bash
stestlogname="`date +%Y%m%d%H%M%S`_stest.log"
stest_server=""
docker_num_in_67=`ssh -p 22008 -t java-tron@47.94.231.67 'docker ps -a | wc -l'`
docker_num_in_67=`echo $docker_num_in_67 | tr -d "\r"`
docker_num_in_122=`ssh -p 22008 -t java-tron@47.94.10.122 'docker ps -a | wc -l'`
docker_num_in_122=`echo $docker_num_in_122 | tr -d "\r"`
if [ $docker_num_in_67 -le $docker_num_in_122 ];
  then
  docker_num=$docker_num_in_67
  stest_server=47.94.231.67
  else
    docker_num=$docker_num_in_122
    stest_server=47.94.10.122
fi

if [[ ${docker_num} -le 3 ]];
then
echo $stest_server
else
    stest_server=""
  fi

if [ "$stest_server" = "" ]
then
echo "All docker server is busy, stest FAILED"
exit 1
fi

ssh java-tron@$stest_server -p 22008 'source ~/.bash_profile && cd /data/workspace/docker_workspace/conf && cat solidity1_config.conf'