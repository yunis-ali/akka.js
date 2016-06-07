![Akka.Js](https://github.com/unicredit/akka.js/blob/merge-js/logo/akkajs.png)

This repository is an ongoing effort to port Akka to the JavaScript runtime, thanks to [Scala.js](http://scala-js.org)

## Build it

To work with the very last version you can compile and publish local:
```
$ git clone https://github.com/unicredit/akka.js
$ cd akka.js
$ git submodule init
$ git submodule update
$ sbt akkaActorJS/publishLocal
```

## Use it

First of all you need to setup a new [Scala.js project](https://www.scala-js.org/doc/project/).
Then add to your JS project configuration:
```scala
resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies += "akka.js" %%% "akkaactor" % "0.1.1-SNAPSHOT"
```

At this point you can use most of the Akka core Api.
Please note that you have to provide a specific configuration during ActorSystem creation.
An example could be:
```scala
import com.typesafe.config.ConfigFactory

val config = ConfigFactory.parseString("""
akka {
home = ""
version = "2.4-SNAPSHOT"
loggers = ["akka.event.JSDefaultLogger"]
logging-filter = "akka.event.JSDefaultLoggingFilter"
loggers-dispatcher = "akka.actor.default-dispatcher"
logger-startup-timeout = 5s
loglevel = "INFO"
stdout-loglevel = "DEBUG"
log-config-on-start = off
log-dead-letters = 0
log-dead-letters-during-shutdown = off

actor {
  provider = "akka.actor.JSLocalActorRefProvider"
  guardian-supervisor-strategy = "akka.actor.DefaultSupervisorStrategy"

  debug {
    receive = off
    autoreceive = off
    lifecycle = off
    event-stream = off
    unhandled = off
  }
}
scheduler {
  implementation = akka.actor.EventLoopScheduler
}
}
""")

val system = ActorSystem("akkajsapp", config)
```
You now can use Akka as described in the official [docs](http://doc.akka.io/docs/akka/snapshot/scala.html).

Please consider that only akka-core is available and on Javascript VM you are in a limited environment.

## Design documentation

The BSc thesis detailing most of the work and the approach taken can be found [here](pdf/thesis.pdf)

The original codebase derives from Sébastien Doeraene's `scala-js-actors`, you can find his original report [here](http://lampwww.epfl.ch/~doeraene/scalajs-actors-design.pdf).

## Akka version

As of now, code is taken from Akka MASTER

## License

Akka.js is distributed under the
[Scala License](http://www.scala-lang.org/license.html).
