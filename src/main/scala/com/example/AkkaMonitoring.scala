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
  case class Greet(system: ActorSystem)
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
    case Greet(system) =>
      //#greeter-send-message
      printerActor ! Greeting(greeting, system)
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
  final case class Greeting(greeting: String, system: ActorSystem)
}
//#printer-messages
//#printer-companion
//#printer-actor
class Printer extends Actor with ActorLogging {
  import Printer._
  implicit val ec: ExecutionContext = context.dispatcher
  private val histo = Kamon.histogram("Greeting_Interval_hist2", MeasurementUnit.time.milliseconds /*, Some(new DynamicRange(1, 1500, 0))*/)
  private val gauge = Kamon.gauge("Greeting_Interval_gauge2")

  def receive = {
    case Greeting(greeting, system) =>
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
      //   projection
      // (akka_actor_processing_time_seconds_sum{path="helloAkka/user/printerActor"} -(akka_actor_processing_time_seconds_sum{path="helloAkka/user/printerActor"} offset 1m))/(akka_actor_processing_time_seconds_count{path="helloAkka/user/printerActor"}- (akka_actor_processing_time_seconds_count{path="helloAkka/user/printerActor"} offset 1m))
      //  mail box size for the last minute
      //  (akka_actor_mailbox_size_sum{path="helloAkka/user/printerActor"} -(akka_actor_mailbox_size_sum{path="helloAkka/user/printerActor"} offset 1m))/(akka_actor_mailbox_size_count{path="helloAkka/user/printerActor"}- (akka_actor_mailbox_size_count{path="helloAkka/user/printerActor"} offset 1m))
      // executor threads
      //      (executor_threads_sum - executor_threads_sum offset 1m)/(executor_threads_count - executor_threads_count offset 1m)
      // фильтровать можно по атрибутам из попапа на графике прометеуса
      Future {
        Thread.sleep(900)
      }
      Future {
        Thread.sleep(900)
      }
      Future {
        Thread.sleep(900)
      }
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

  val allGreeters = Vector(howdyGreeter, helloGreeter, goodDayGreeter)

  def randomGreeter = helloGreeter

  Kamon.addReporter(new PrometheusReporter())
  Kamon.addReporter(new ZipkinReporter())
  //  for (i <- 1 to 60) {
  println(s"service ${Kamon.environment.service}")
  while (true) {
    randomGreeter ! Greet(system)
    Thread.sleep(600)
  }
}
