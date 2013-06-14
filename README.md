# StormCell

*Metrics for your storm cluster*

StormCell integrates [Metrics](http://metrics.codahale.com/) and [Storm](http://storm-project.net/), providing a simple web console for your Storm workers.

## Features

HTTP routes available for each worker process:

* `/metrics`: Prints all metrics for the VM. Supports `?pretty=true`.
* `/healthcheck`: Runs all registered healthchecks for the VM. Supports `?pretty=true`.
* `/threads`: Dumps all threads in the VM.
* `/ping`: Prints "pong" if the process is alive.

## Setup

* Clone the source
* Run `mvn package` in the project directory
* Copy `target/stormcell-0.2.0.jar` to your supervisor nodes
* Edit `storm.yaml` and add `stormcell-0.2.0.jar` or its containing folder to `java.library.path`
* Edit `storm.yaml` and add `topology.auto.task.hooks: "org.jodah.stormcell.StormCellTaskHook"`
* Restart your supervisors

## Configuration

By default, the web console ports for each worker will be based off of the worker port numbers, starting at 7000:

```
webConsolePort = 7000 + (workerPort % 100)
```

To configure specific web console ports for each worker, edit `storm.yaml` to include `worker.webconsole.ports` that correspond to each worker port (keyed by worker port):

```
worker.webconsole.ports:
  6700: 7000
  6701: 7001
  6702: 7002
  6703: 7003
```

## Thanks

Thanks for Ooyala for the [initial concept](https://github.com/ooyala/metrics_storm).

## License

Copyright 2013 Jonathan Halterman - Released under the [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0.html).