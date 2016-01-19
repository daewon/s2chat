Pub/Sub chat using akka cluster

```
ex
sbt run -Dhttp.port=9001 -Dakka.remote.netty.tcp.port=2551 -J-Xmx2048M
sbt run -Dhttp.port=9002 -Dakka.remote.netty.tcp.port=2552 -J-Xmx2048M
```

