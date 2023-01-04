package fr.acinq.alarmbot

import akka.actor.DiagnosticActorLogging
import fr.acinq.eclair.{Kit, Setup}
import org.json4s.Serialization
import org.json4s.jackson.Serialization
import sttp.client3._

import scala.util.{Failure, Success, Try}

// requests

class ExternalHedgeClient(kit: Kit, setup: Setup, pluginConfig: AlarmBotConfig) extends DiagnosticActorLogging {
  val kolliderClient = new KolliderClient(pluginConfig)

  import setup.ec
  implicit val sttpBackend = SttpUtil.createSttpBackend(kit.nodeParams.socksProxy_opt, pluginConfig.useProxy, log)

  implicit val serialization: Serialization = Serialization

  context.system.eventStream.subscribe(channel = classOf[ExternalHedgeMessage], subscriber = self)

  def logReport[T](tag: String): PartialFunction[Try[Response[T]], Unit] = {
    case Failure(reason) => log.info(s"PLGN AlarmBot, failed to execute '$tag', reason: ${reason.getMessage}")
    case Success(response) => log.info(s"PLGN AlarmBot, execute '$tag' successfully, response code=${response.code}, body=${response.body}")
  }

  override def preStart(): Unit = {
    log.info(s"Launching hedge bot")
    kolliderClient.checkAvailability()(setup.sttpBackend, setup.ec).onComplete(logReport("preStart"))
  }

  override def receive: Receive = {
    case msg: ExternalHedgeMessage => {
      //kolliderClient.sendMessage(msg.amount, msg.rate).onComplete(logReport("ExternalHedgeMessage"))
      kolliderClient.addPosition(msg.channel, msg.amount, msg.rate).onComplete(logReport("ExternalHedgeMessage"))
    }
  }
}