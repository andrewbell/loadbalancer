# loadbalancer

A basic load balancer written in Java that demonstrates how to load balance TCP connections across
a given set of backend servers. This is a level 4 load balancer and works on hostname and port only.
Socket data is routed directly to BE servers without any inspection of protocols or packets. LB will
also detect if a backend server has gone down and mark it as offline and avoid routing traffic to it.
It will also attempt to self-heal and bring back offline servers by occasionally pinging them.

The LB currently implements two load balancing strategies, round-robin and random to demonstrate the use of
dependency injection to decouple load balancer logic and server selection logic.

Some JUnit5 tests have been included to show how we can use dependency injection to mock dependencies, making tests easier to write (see `LoadBalancerTest.java`)

Config, Ping and Socket creation are moved behind their own interfaces/factories to facilitate easier testing.


**Potential improvements:**

- Add a suite of load tests to see how the LB handles heavier workloads, in particular thousands of random client connections and long-lived connections
- Change it to a level 7 LB so we can do things like TLS termination and inspection of HTTP headers to route traffic based on Host header
- Implement SLF4J logging, at the moment we use println which are ok for development, but should not be deployed to a production environment
- Extend the LB to support UDP/datagram packets
- More load-balancing algorithms, for example Weighted Round-Robin, IP hash, Least Connections or Least Response Time (*)
- Health monitoring, e.g. what to alert when all BE servers are down

**To build:**
```
./gradlew clean build
```

**To run:**
```
./gradlew :lb:run --console=plain --args "8080 localhost:8050,localhost:8051,localhost:8052"
```

**To test:**

Use netcat `nc` command to listen on three ports, using separate Shell windows:
```
nc -kl 8050
nc -kl 8051
nc -kl 8052
```
Then use a command like `telnet` to send some data to the LB on port 8080.
If successful you will see the data appear on the appropriate terminal.
Then quit telnet and restart it to create a new TCP connection to see
socket data load balanced to a different shell.
```
telnet localhost 8080
```
**Example**

![Example screen shot](readme/lb_screenshot.png?raw=true "Example")

**Source Overview**


![Source overview](readme/src_overview.png?raw=true "Source Overview")

Project uses standard Gradle layout. A gradle wrapper is included in the repo for easier compiling. Java 24 required.

```
lb/src/main/java
lb/src/main/test
```

Main program and load balancer classes below. Entry point and argument parsing done in `Main.java`.
Core program is in `LoadBalancer.java`. Config at `LoadBalancerConfig.java`.
```
lb/src/main/java/org/example/loadbalancer/lb/LoadBalancer.java
lb/src/main/java/org/example/loadbalancer/lb/LoadBalancerPing.java
lb/src/main/java/org/example/loadbalancer/lb/Main.java
lb/src/main/java/org/example/loadbalancer/lb/LoadBalancerConfig.java
```

Classes used for the creation of sockets below. Main socket handler is `SocketHandlerThread.java`
```
lb/src/main/java/org/example/loadbalancer/lb/socket/SocketHandlerThreadFactory.java
lb/src/main/java/org/example/loadbalancer/lb/socket/SocketHandlerThread.java
lb/src/main/java/org/example/loadbalancer/lb/socket/SocketHandler.java
lb/src/main/java/org/example/loadbalancer/lb/socket/SocketHandlerThreadAbstractFactory.java
```
Classes used for the creation load balancing algorithms below. Main factory for creating these is `LoadBalancerStrategyFactory.java`
```
lb/src/main/java/org/example/loadbalancer/lb/strategy/algorithms/RoundRobinLoadBalancerStrategy.java
lb/src/main/java/org/example/loadbalancer/lb/strategy/algorithms/RandomLoadBalancerStrategy.java
lb/src/main/java/org/example/loadbalancer/lb/strategy/LoadBalancerStrategyFactory.java
lb/src/main/java/org/example/loadbalancer/lb/strategy/LoadBalancerStrategy.java
lb/src/main/java/org/example/loadbalancer/lb/strategy/LoadBalancerStrategyType.java
```

Various utility classes to remove boilerplate below:
```
lb/src/main/java/org/example/loadbalancer/lb/util/Ping.java
lb/src/main/java/org/example/loadbalancer/lb/util/Helper.java
```