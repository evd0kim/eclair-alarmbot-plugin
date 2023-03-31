package fr.acinq.alarmbot

import sttp.client3.{Response}
import akka.actor.DiagnosticActorLogging

import scala.util.{Failure, Success, Try}
import fr.acinq.eclair.{Kit, Setup}
import org.json4s.Serialization
import org.json4s.jackson.Serialization

class ExternalHedgeClient(kit: Kit, setup: Setup, pluginConfig: AlarmBotConfig) extends DiagnosticActorLogging {
  val kolliderClient = new KolliderClient(pluginConfig)

  val tgbot = new Messenger(pluginConfig)

  import setup.{ec, sttpBackend}
  implicit val serialization: Serialization = Serialization

  context.system.eventStream.subscribe(channel = classOf[ExternalHedgeMessage], subscriber = self)

  def logReport[T](tag: String): PartialFunction[Try[Response[T]], Unit] = {
    case Failure(reason) => log.info(s"PLGN AlarmBot, failed to execute '$tag', reason: ${reason.getMessage}")
    case Success(response) => log.info(s"PLGN AlarmBot, execute '$tag' successfully, response code=${response.code}, body=${response.body}")
  }

  override def preStart(): Unit = {
    log.info(s"launching hedge bot")
    pluginConfig.hedgeServicesMap.foreach {
      case(ticker, host) => {
        log.info(s"checking $ticker hedging service on $host")
        kolliderClient.checkAvailability(host).onComplete(logReport("preStart"))
      }
    }
  }

  override def receive: Receive = {
    case msg: ExternalHedgeMessage if pluginConfig.hedgeServicesMap.contains(msg.ticker) =>
      kolliderClient.addPosition(msg.channel, msg.localUpdates, msg.remoteUpdates, msg.ticker, msg.amount, msg.rate).onComplete(logReport("ExternalHedgeMessage"))
      if (pluginConfig.hedgeNotify) {
        tgbot.sendMessage(s"${msg.channel}\n${msg.ticker}\n${msg.amount}\n${msg.rate}")
      }
    case msg: ExternalHedgeMessage =>
      log.error(s"ignoring unknown ExternalHedgeMessage with ${msg.ticker}")
  }
}