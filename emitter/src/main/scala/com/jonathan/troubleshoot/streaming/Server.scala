package com.jonathan.troubleshoot.streaming

import akka.actor.ActorSystem
import akka.event.{Logging, MarkerLoggingAdapter}
import akka.grpc.GrpcClientSettings
import akka.stream.Materializer
import cats.Monad
import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.implicits._

import java.time.{Duration, Instant}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration => ScalaDuration}
import scala.util.Random

object Server extends IOApp {
  private val resources = for {
    system <- Resource.liftF(IO(ActorSystem("foo-emitter-test")))
    mat    <- Resource.liftF(IO(Materializer(system)))
  } yield (system, mat)

  def run(args: List[String]): IO[ExitCode] = {
    val duration =
      args match {
        case time :: units :: Nil =>
          Duration.ofMillis(ScalaDuration.create(s"$time $units").toMillis)
        case _ =>
          Duration.ofSeconds(5)
      }

    resources.use {
      case (actorSystem, materializer) =>
        val logger = Logging.withMarker(actorSystem, Server.getClass)
        for {
          _ <- run(logger)(duration)(
            actorSystem,
            materializer,
            scala.concurrent.ExecutionContext.Implicits.global
          )
          exitCode <- IO.never
        } yield exitCode
    }
  }
  def run(logger: MarkerLoggingAdapter)(
      duration: java.time.Duration
  )(implicit
      actorSystem: ActorSystem,
      materializer: Materializer,
      executionContext: ExecutionContext
  ): IO[Any] = {
    val grpcClientSettings: GrpcClientSettings =
      GrpcClientSettings
        .fromConfig("com.jonathan.troubleshoot.streaming.EventService")
    val eventBus = new EventBus(EventServiceClient(grpcClientSettings))

    val `end time` = Instant.now plus duration

    Monad[IO].whileM_ {
      IO {
        Instant.now isBefore `end time`
      }
    } {
      IO(logger.info("Publishing event...")) *> {
        for {
          payload <- IO {
            FooEvent(
              source = "test-source",
              service = "probe",
              responseTime = Random.nextLong(),
              error = Some(
                MyErrorDetails(
                  code = ((1 to 7) map (_ => Random.nextPrintableChar)).mkString
                )
              )
            )
          }
          result <- eventBus.publish(payload)
        } yield result
      }
    }
  }
}
