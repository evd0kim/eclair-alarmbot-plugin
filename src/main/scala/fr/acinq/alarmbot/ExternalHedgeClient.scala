package fr.acinq.alarmbot

import akka.actor.DiagnosticActorLogging
import com.softwaremill.sttp.Response
import fr.acinq.eclair.{Kit, Setup}

import scala.util.{Failure, Success, Try}


class ExternalHedgeClient(kit: Kit, setup: Setup, pluginConfig: AlarmBotConfig) extends DiagnosticActorLogging {
  context.system.eventStream.subscribe(channel = classOf[ExternalHedgeMessage], subscriber = self)

  def logReport(tag: String): PartialFunction[Try[Response[String]], Unit] = {
    case Failure(reason) => log.info(s"PLGN AlarmBot, failed to execute '$tag', reason: ${reason.getMessage}")
    case Success(response) => log.info(s"PLGN AlarmBot, execute '$tag' successfully, response code=${response.code}, body=${response.body}")
  }

  override def preStart(): Unit = log.info(s"Launching hedge bot")

  override def receive: Receive = {
    case msg: ExternalHedgeMessage => println(s"${msg.senderEntity}: ${msg.amount} ${msg.rate}")
  }
}