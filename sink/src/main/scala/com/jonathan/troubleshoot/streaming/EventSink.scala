package com.jonathan.troubleshoot.streaming

import akka.NotUsed
import akka.actor.{ActorSystem => ClassicActorSystem}
import akka.event.Logging
import akka.grpc.scaladsl.Metadata
import akka.stream.Attributes
import akka.stream.Attributes.CancellationStrategy
import akka.stream.scaladsl.{MergeHub, Sink, Source}
import akka.util.Timeout
import cats.effect.{ContextShift, IO}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class EventSink(implicit classicActorSystem: ClassicActorSystem, contextShift: ContextShift[IO])
    extends EventServicePowerApi {
  private val logger = Logging.withMarker(classicActorSystem, classOf[EventSink])

  private implicit val timeout: Timeout = 10.seconds

  val sink: Sink[FooEvent, NotUsed] =
    (MergeHub.source[FooEvent] named "Test Sink") to {
      Sink.foreach { event =>
        logger.debug("Received {}", event)
      }
    } run

  override def publish(
      in: Source[FooEvent, NotUsed],
      metadata: Metadata
  ): Future[Received] = {
    logger.debug("Source in {}", in)
    in withAttributes (Attributes
      .logLevels(
        onElement = Logging.WarningLevel,
        onFinish = Logging.InfoLevel,
        onFailure = Logging.DebugLevel
      ) and CancellationStrategy(CancellationStrategy.PropagateFailure)) runWith sink
    Future.successful(Received())
  }
}
