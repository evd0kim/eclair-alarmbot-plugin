package fr.acinq.alarmbot

import akka.actor.DiagnosticActorLogging
import com.softwaremill.sttp._
import fr.acinq.eclair.Setup
import fr.acinq.eclair.blockchain.bitcoind.zmq.ZMQActor.{ZMQConnected, ZMQDisconnected, ZMQEvent}
import fr.acinq.eclair.blockchain.watchdogs.BlockchainWatchdog.DangerousBlocksSkew
import fr.acinq.eclair.channel.{ChannelClosed, ChannelStateChanged, NORMAL, WAIT_FOR_FUNDING_LOCKED}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


class WatchdogSync(setup: Setup, botApiKey: String, chatId: String) extends DiagnosticActorLogging {
  context.system.eventStream.subscribe(channel = classOf[CustomAlarmBotMessage], subscriber = self)
  context.system.eventStream.subscribe(channel = classOf[DangerousBlocksSkew], subscriber = self)
  context.system.eventStream.subscribe(channel = classOf[ChannelStateChanged], subscriber = self)
  context.system.eventStream.subscribe(channel = classOf[ChannelClosed], subscriber = self)
  context.system.eventStream.subscribe(channel = classOf[ZMQEvent], subscriber = self)

  val readTimeout: FiniteDuration = 10.seconds

  val baseUri: Uri = uri"https://api.telegram.org/bot$botApiKey/sendMessage"

  private def sendMessage(message: String)(implicit http: SttpBackend[Future, Nothing], ec: ExecutionContext) = {
    val parametrizedUri = baseUri.params("chat_id" -> chatId, "text" -> message, "parse_mode" -> "HTML")
    sttp.readTimeout(readTimeout).get(parametrizedUri).send.map(identity)
  }

  def logReport(tag: String): PartialFunction[Try[Response[String]], Unit] = {
    case Failure(reason) => log.info(s"PLGN AlarmBot, failed to send '$tag', reason: ${reason.getMessage}")
    case Success(response) => log.info(s"PLGN AlarmBot, sent '$tag' successfully, response code=${response.code}, body=${response.body}")
  }

  import setup.{ec, sttpBackend}

  override def preStart(): Unit = sendMessage("Node runs").onComplete(logReport("preStart"))

  override def receive: Receive = {
    case ChannelStateChanged(_, channelId, _, remoteNodeId, WAIT_FOR_FUNDING_LOCKED, NORMAL, commitsOpt) =>
      val details = commitsOpt.map(commtis => s"capacity: ${commtis.capacity}, announceChannel: ${commtis.announceChannel}")
      sendMessage(s"New channel established, remoteNodeId: $remoteNodeId, channelId: $channelId, ${details.orNull}").onComplete(logReport("ChannelStateChanged"))

    case ChannelClosed(_, channelId, closingType, _) =>
      sendMessage(s"Channel closed, channelId: $channelId, closingType: ${closingType.getClass.getName}").onComplete(logReport("ChannelClosed"))

    case ZMQConnected =>
      sendMessage("ZMQ connection UP").onComplete(logReport("ZMQConnected"))

    case ZMQDisconnected =>
      sendMessage("ZMQ connection DOWN").onComplete(logReport("ZMQDisconnected"))

    case msg: DangerousBlocksSkew =>
      sendMessage(s"DangerousBlocksSkew from ${msg.recentHeaders.source}").onComplete(logReport("DangerousBlocksSkew"))

    case msg: CustomAlarmBotMessage =>
      sendMessage(s"${msg.senderEntity}: ${msg.message}").onComplete(logReport("CustomAlarmBotMessage"))
  }
}