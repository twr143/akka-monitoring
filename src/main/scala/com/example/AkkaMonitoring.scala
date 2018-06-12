//#full-example
package com.example
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.Patterns.after
import kamon.Kamon
import kamon.metric.{DynamicRange, MeasurementUnit}
import kamon.prometheus.PrometheusReporter
import kamon.zipkin.ZipkinReporter

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Random
//#greeter-companion
//#greeter-messages
object Greeter {
  //#greeter-messages
  def props(message: String, printerActor: ActorRef): Props = Props(new Greeter(message, printerActor))
  //#greeter-messages
  final case class WhoToGreet(who: String)
  case object Greet
}
//#greeter-messages
//#greeter-companion
//#greeter-actor
class Greeter(message: String, printerActor: ActorRef) extends Actor {
  import Greeter._
  import Printer._
  var greeting = ""

  def receive = {
    case WhoToGreet(who) =>
      greeting = message + ", " + who
    case Greet =>
      //#greeter-send-message
      printerActor ! Greeting(greeting)
    //#greeter-send-message
  }
}
//#greeter-actor
//#printer-companion
//#printer-messages
object Printer {
  //#printer-messages
  def props: Props = Props[Printer]
  //#printer-messages
  final case class Greeting(greeting: String)
}
//#printer-messages
//#printer-companion
//#printer-actor
class Printer extends Actor with ActorLogging {
  import Printer._
  implicit val ec: ExecutionContext = context.dispatcher
  implicit val system = ActorSystem()
  private val histo = Kamon.histogram("Greeting_Interval_hist2", MeasurementUnit.time.milliseconds /*, Some(new DynamicRange(1, 1500, 0))*/)
  private val gauge = Kamon.gauge("Greeting_Interval_gauge2")

  def receive = {
    case Greeting(greeting) =>
      val start = System.currentTimeMillis()
      val span = Kamon.buildSpan(s"printer-span-${UUID.randomUUID()}").start()
      //      val startedTimer = Kamon.timer("Greeting_Interval").start()
      val interval = 500 + Random.nextInt(8) * 100
      log.info("interval {}", interval)
      Thread.sleep(interval)
      //      log.info("Greeting received (from " + sender() + "): " + greeting)
      //      startedTimer.stop()
      span.finish()
      val end = System.currentTimeMillis()
      //      histo.record(end - start, 1)
      val tmp = end - start
      histo.record(tmp) //реальный массив не хранится в гистограмме, только стат  распрделение, сколько точек в какой отсек попало
    //    prometheus queries:
    //
    // (Greeting_Interval_hist2_seconds_sum - (Greeting_Interval_hist2_seconds_sum offset 1m))/(Greeting_Interval_hist2_seconds_count - (Greeting_Interval_hist2_seconds_count offset 1m))
    //  (akka_actor_processing_time_seconds_sum -(akka_actor_processing_time_seconds_sum offset 1m))/(akka_actor_processing_time_seconds_count- (akka_actor_processing_time_seconds_count offset 1m))
  }
}
//#printer-actor
//#main-class
object AkkaMonitoring extends App {
  import Greeter._
  // Create the 'helloAkka' actor system
  val system: ActorSystem = ActorSystem("helloAkka")
  //#create-actors
  // Create the printer actor
  val printer: ActorRef = system.actorOf(Printer.props, "printerActor")
  // Create the 'greeter' actors
  val howdyGreeter: ActorRef =
    system.actorOf(Greeter.props("Howdy", printer), "howdyGreeter")
  val helloGreeter: ActorRef =
    system.actorOf(Greeter.props("Hello", printer), "helloGreeter")
  val goodDayGreeter: ActorRef =
    system.actorOf(Greeter.props("Good day", printer), "goodDayGreeter")
  //#create-actors
  //#main-send-messages
  //  howdyGreeter ! WhoToGreet("Akka")
  //  howdyGreeter ! Greet
  //
  //  howdyGreeter ! WhoToGreet("Lightbend")
  //  howdyGreeter ! Greet
  //
  //  helloGreeter ! WhoToGreet("Scala")
  //  helloGreeter ! Greet
  //
  //  goodDayGreeter ! WhoToGreet("Play")
  //  goodDayGreeter ! Greet
  //#main-send-messages
  val allGreeters = Vector(howdyGreeter, helloGreeter, goodDayGreeter)

  def randomGreeter = helloGreeter

  Kamon.addReporter(new PrometheusReporter())
  Kamon.addReporter(new ZipkinReporter())
  //  for (i <- 1 to 60) {
  println(s"service ${Kamon.environment.service}")
  while (true) {
    randomGreeter ! Greet
    Thread.sleep(600)
  }
  //  }                     // only general stat info
}
//#main-class
//#full-example
