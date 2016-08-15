## Performance Test using High Performance Netty Back-End

In this performance test, five instances of simple service created using Netty framework were used.  Each instance is a fast backend (0s delay) with response of size 1KB.
See [netty backend] (services/Netty) for more details.  Performance bench-marks were done between [open source Nginx load balancer] (nginx) and [this LB] (gw-lb) on [Ubuntu VM] (test-bed).
1,000,000 requests were sent at different concurrency levels (500 to 12,000) to Netty backend, Nginx and load balancer using apache bench via this [automated script] (excecute-tests.sh).

## Prerequisite
* **apache2-utils** - This performance tests are executed using ApacheBench. Therefore in order to run the tests, apache2-utils
should be installed in the machine.

Run all tests using the following command from [performance-benchmark](performance-benchmark)

```
./run.sh [load-balancer-endpoint]
```

`Example: ./run.sh http://localhost:8290/stocks`

## Throughput Test

Tests were done twice.  Average of 'Average throughput' for each concurrency level is calculated and plotted.  

![Throughput] (graphs/throughput_without_netty.png)

![ThroughputWithNetty] (graphs/throughput_with_netty.png)

## Latency Test

Tests were done twice.  Average of 'Mean Latency' for each concurrency level is calculated and plotted.

![MeanLatency] (graphs/mean_latency.png)
 
## Reference  

https://github.com/wso2/msf4j/tree/master/perf-benchmark

Scripts available in the above mentioned repo were customized for this project.

