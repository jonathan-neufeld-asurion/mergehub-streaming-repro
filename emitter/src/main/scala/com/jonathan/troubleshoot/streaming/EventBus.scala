package com.jonathan.troubleshoot.streaming

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{OverflowStrategy, QueueOfferResult}
import cats.effect.{ContextShift, IO}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DurationInt, FiniteDuration}

case class ScheduledFrame(size: Int, period: FiniteDuration)

class EventBus(
    eventService: EventServiceClient,
    queueSize: Int = 2048,
    bufferFrame: ScheduledFrame = ScheduledFrame(2048, 500.millis),
    throttleFrame: ScheduledFrame = ScheduledFrame(5, 50.millis)
)(implicit
    system: ActorSystem,
    contextShift: ContextShift[IO],
    executionContext: ExecutionContext
) {
  private val logger = Logging.withMarker(system, classOf[EventBus])

  private val queue =
    Source
      .queue[FooEvent](queueSize, OverflowStrategy.backpressure)
      .map {
        event =>
          logger.debug("Publishing event {}", event)
          event
      }
      .groupedWithin(bufferFrame.size, bufferFrame.period)
      .map {
        chunk =>
          logger.debug("Grouped {} events", chunk.size)
          chunk
      }
      .map(
        Source(_)
      )
      .throttle(throttleFrame.size, throttleFrame.period)
      .mapAsyncUnordered(throttleFrame.size) { eventSource =>
        logger.debug("Pushing source {}", eventSource)
        eventService
          .publish()
          .invoke(eventSource)
          .map(_ => eventSource)
      }
      .to(Sink.ignore)
      .run()

  def publish(event: FooEvent): IO[Unit] = {
    IO.fromFuture(IO(queue.offer(event))) flatMap {
      case QueueOfferResult.Enqueued    => IO.pure(())
      case QueueOfferResult.Dropped     => IO.raiseError(new IllegalStateException("Dropped"))
      case QueueOfferResult.Failure(ex) => IO.raiseError(ex)
      case QueueOfferResult.QueueClosed => IO.raiseError(new IllegalStateException("Closed"))
    }
  }
}
