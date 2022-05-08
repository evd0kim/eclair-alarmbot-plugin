package fr.acinq.alarmbot

import sttp.client3.{asString, Response, SttpBackend, UriContext, basicRequest}
import akka.actor.DiagnosticActorLogging

import scala.util.{Failure, Success, Try}
import fr.acinq.eclair.{Kit, MilliSatoshi, Setup}
import org.json4s.JsonAST.JObject
import org.json4s.Serialization
import org.json4s.jackson.Serialization

// requests
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import sttp.client3.{HttpURLConnectionBackend, Response, SttpBackend, UriContext, basicRequest}

class ExternalHedgeClient(kit: Kit, setup: Setup, pluginConfig: AlarmBotConfig) extends DiagnosticActorLogging {
  val kolliderClient = new KolliderClient(pluginConfig)

  import setup.{ec, sttpBackend}
  implicit val serialization: Serialization = Serialization

  context.system.eventStream.subscribe(channel = classOf[ExternalHedgeMessage], subscriber = self)

  def logReport[T](tag: String): PartialFunction[Try[Response[T]], Unit] = {
    case Failure(reason) => log.info(s"PLGN AlarmBot, failed to execute '$tag', reason: ${reason.getMessage}")
    case Success(response) => log.info(s"PLGN AlarmBot, execute '$tag' successfully, response code=${response.code}, body=${response.body}")
  }

  override def preStart(): Unit = {
    log.info(s"Launching hedge bot")
    kolliderClient.checkAvailability().onComplete(logReport("preStart"))
  }

  override def receive: Receive = {
    case msg: ExternalHedgeMessage => {
      //kolliderClient.sendMessage(msg.amount, msg.rate).onComplete(logReport("ExternalHedgeMessage"))
      kolliderClient.addPosition(msg.channel, msg.amount, msg.rate).onComplete(logReport("ExternalHedgeMessage"))
    }
  }
}