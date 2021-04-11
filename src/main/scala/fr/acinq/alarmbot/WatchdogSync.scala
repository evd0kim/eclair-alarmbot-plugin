package fr.acinq.alarmbot

import scala.concurrent.duration._
import com.softwaremill.sttp._
import akka.actor.{Actor, DiagnosticActorLogging}
import fr.acinq.eclair.{Kit, Setup}
import fr.acinq.eclair.blockchain.watchdogs.BlockchainWatchdog.DangerousBlocksSkew
import com.softwaremill.sttp.SttpBackend
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait Messenger {
  val botApiKey: String = AlarmBotConfig.bot

  val chatId: String = AlarmBotConfig.chat

  val readTimeout: FiniteDuration = 10.seconds

  val baseUri: Uri = uri"https://api.telegram.org/bot$botApiKey/sendMessage"

  def sendMessage(message: String)(implicit http: SttpBackend[Future, Nothing], ec: ExecutionContext): Future[StatusCode] = {
    val parametrizedUri = baseUri.params("chat_id" -> chatId, "text" -> message, "parse_mode" -> "MarkdownV2")
    sttp.readTimeout(readTimeout).get(parametrizedUri).send.map(_.code)
  }
}

class WatchdogSync(kit: Kit, setup: Setup) extends DiagnosticActorLogging with Messenger {
  context.system.eventStream.subscribe(channel = classOf[DangerousBlocksSkew], subscriber = self)

  import setup.{ec, sttpBackend}

  override def preStart(): Unit = sendMessage("Node *runs*").onComplete {
    case Failure(reason) => log.info(s"PLGN AlarmBot, failed to send message on preStart, reason: ${reason.getMessage}")
    case Success(statusCode) => log.info(s"PLGN AlarmBot, alarmbot sent message successfully, response code was $statusCode")
  }

  override def receive: Receive = {
    case _: DangerousBlocksSkew => sendMessage("Alarmbot received *DangerousBlocksSkew*")
  }
}