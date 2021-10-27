package fr.acinq.alarmbot

import com.softwaremill.sttp.{Response}
import akka.actor.DiagnosticActorLogging
import fr.acinq.eclair.{Kit, Setup}
import fr.acinq.eclair.blockchain.watchdogs.BlockchainWatchdog.DangerousBlocksSkew
import fr.acinq.eclair.blockchain.bitcoind.zmq.ZMQActor.{ZMQConnected, ZMQDisconnected, ZMQEvent}
import fr.acinq.eclair.channel.{ChannelClosed, ChannelStateChanged, NORMAL, WAIT_FOR_FUNDING_LOCKED}

import scala.util.{Failure, Success, Try}


class WatchdogSync(kit: Kit, setup: Setup, pluginConfig: AlarmBotConfig) extends DiagnosticActorLogging {
  val tgbot = new Messenger(pluginConfig)

  context.system.eventStream.subscribe(channel = classOf[CustomAlarmBotMessage], subscriber = self)
  context.system.eventStream.subscribe(channel = classOf[DangerousBlocksSkew], subscriber = self)
  context.system.eventStream.subscribe(channel = classOf[ChannelStateChanged], subscriber = self)
  context.system.eventStream.subscribe(channel = classOf[ChannelClosed], subscriber = self)
  context.system.eventStream.subscribe(channel = classOf[ZMQEvent], subscriber = self)

  import setup.{ec, sttpBackend}

  def logReport(tag: String): PartialFunction[Try[Response[String]], Unit] = {
    case Failure(reason) => log.info(s"PLGN AlarmBot, failed to send '$tag', reason: ${reason.getMessage}")
    case Success(response) => log.info(s"PLGN AlarmBot, sent '$tag' successfully, response code=${response.code}, body=${response.body}")
  }

  override def preStart(): Unit = tgbot.sendMessage("Node runs").onComplete(logReport("preStart"))

  override def receive: Receive = {
    case ChannelStateChanged(_, channelId, _, remoteNodeId, WAIT_FOR_FUNDING_LOCKED, NORMAL, commitsOpt) =>
      val details = commitsOpt.map(commtis => s"capacity: ${commtis.capacity}, announceChannel: ${commtis.announceChannel}")
      tgbot.sendMessage(s"New channel established, remoteNodeId: $remoteNodeId, channelId: $channelId, ${details.orNull}").onComplete(logReport("ChannelStateChanged"))

    case ChannelClosed(_, channelId, closingType, _) =>
      tgbot.sendMessage(s"Channel closed, channelId: $channelId, closingType: ${closingType.getClass.getName}").onComplete(logReport("ChannelClosed"))

    case ZMQConnected =>
      tgbot.sendMessage("ZMQ connection UP").onComplete(logReport("ZMQConnected"))

    case ZMQDisconnected =>
      tgbot.sendMessage("ZMQ connection DOWN").onComplete(logReport("ZMQDisconnected"))

    case msg: DangerousBlocksSkew =>
      tgbot.sendMessage(s"DangerousBlocksSkew from ${msg.recentHeaders.source}").onComplete(logReport("DangerousBlocksSkew"))

    case msg: CustomAlarmBotMessage =>
      tgbot.sendMessage(s"${msg.senderEntity}: ${msg.message}").onComplete(logReport("CustomAlarmBotMessage"))
  }
}