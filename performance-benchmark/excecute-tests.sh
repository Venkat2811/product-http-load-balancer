#!/usr/bin/env bash

baseDir=$(dirname "$0")
concLevels="1 25 50 100 200 400 800"
# 1600 3200"
#perTestTime=30
testLoops=1000
warmUpConc=200
warmUpLoop=50000

tmpDir="$baseDir/tmpData"
timeStmp=$(date +%s)

declare -A MAP


#For GW-LB
service="http://localhost:8290/stocks"
#For Nginx
#service="http://localhost/stockquote/all"


function warmUp(){
echo "Warmup service.."
ab -k -c $warmUpConc -n $warmUpLoop $service #> /dev/null
echo "Warmup service done"
}

function testConcLevel(){	
local concLevel=$1

local resOut="$tmpDir/result-conc$concLevel-time$timeStmp-$(uuidgen)"
local percentOut="$tmpDir/percentile-conc$concLevel-time$timeStmp-$(uuidgen)"

echo "Testing Conc Level : $concLevel"

ab -k -c $concLevel -n $testLoops -e "$percentOut" $service > "$resOut"

local tps=$(cat "$resOut" | grep -Eo "Requests per second.*" | grep -Eo "[0-9]+" | head -1)

local meanLat=$(cat "$resOut" | grep -Eo "Time per request.*\(mean\)" | grep -Eo "[0-9]+(\.[0-9]+)?")

local percents=$(cat "$percentOut" | grep -Eo ",.*" | grep -Eo "[0-9]+(\.[0-9]+)?" | tr '\n' ',')
percents="$concLevel, $percents"

echo "At concurrency $concLevel"

MAP["$concLevel-tps"]=$tps
echo -e "\tThroughput $tps"

MAP["$concLevel-meanLat"]=$meanLat
echo -e "\tMean latency is $meanLat"

MAP["$concLevel-percents"]=$percents
echo -e "\tPercentiles are $percents"

echo "Testing Conc Level $concLevel is done"
}

function iterateConcLevels(){
warmUp # Warming up 

if [ -d "$tmpDir" ]
then
	echo "$tmpDir exists."
else
	mkdir "$tmpDir"
fi	

local concLevel=""
for concLevel in $concLevels
    do
        testConcLevel $concLevel
    done

 

}


iterateConcLevels



