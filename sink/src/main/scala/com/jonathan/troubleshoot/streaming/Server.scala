package com.jonathan.troubleshoot.streaming

import akka.actor.ActorSystem
import akka.event.Logging
import akka.grpc.scaladsl.{ServerReflection, ServiceHandler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import cats.effect.{ContextShift, IO}

import scala.concurrent.{ExecutionContextExecutor, Future}

object Server extends App {
  private implicit val system: ActorSystem            = ActorSystem("foo-sink-test")
  private implicit val ec: ExecutionContextExecutor   = system.dispatcher
  private implicit val contextShift: ContextShift[IO] = IO.contextShift(ec)
  private val logger                                  = Logging.withMarker(system, Server.getClass)

  private lazy val grpcHandler: HttpRequest => Future[HttpResponse] =
    ServiceHandler.concatOrNotFound(
      EventServicePowerApiHandler.partial(new EventSink),
      ServerReflection.partial(List(EventService))
    )

  val binding = Http().newServerAt("localhost", 8099) bind grpcHandler

  binding.foreach { _ =>
    logger.info("Sink is listening")
  }
}
