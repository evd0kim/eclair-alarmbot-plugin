package fr.acinq.alarmbot

import akka.actor.{Actor, DiagnosticActorLogging}
import fr.acinq.eclair.Kit
import fr.acinq.eclair.blockchain.watchdogs.BlockchainWatchdog.DangerousBlocksSkew

import java.net.URLEncoder
import scalaj.http.Http

trait Messenger {
  val botApiKey = AlarmBotConfig.bot
  val chatId = AlarmBotConfig.chat
  val connectionTimeout = 1000 * 10 // in milliseconds
  val readTimeout = 1000 * 10 // in milliseconds

  def sendMessage(message: String): Option[String] = {
    val url = s"https://api.telegram.org/bot$botApiKey/sendMessage" +
      s"?chat_id=${URLEncoder.encode(chatId, "UTF-8")}" +
      s"&parse_mode=MarkdownV2" +
      s"&text=${URLEncoder.encode(message, "UTF-8")}"

    val response = Http(url)
      .param("chat_id", URLEncoder.encode(chatId, "UTF-8"))
      .param("parse_mode", "MarkdownV2")
      .param("text", URLEncoder.encode(message, "UTF-8"))
      .asString

    if (response.is2xx) Some(response.body)
    else None
  }
}

class WatchdogSync(kit: Kit) extends Actor with DiagnosticActorLogging with Messenger {

  log.info(s"starting actor")
  context.system.eventStream.subscribe(channel = classOf[DangerousBlocksSkew], subscriber = self)
  //context.system.eventStream.subscribe(channel = classOf[NewBlock], subscriber = self)
  log.info(s"subscribed")

  override def preStart() = {
    val response: Option[String] = sendMessage("Node *runs*")
    if(response.nonEmpty) {
      log.info("alarmbot sent message successfully")
    } else {
      log.info("failed to send message on preStart")
    }
  }

  override def receive: Receive = {
    case DangerousBlocksSkew(recentHeaders) =>
      val response: Option[String] = sendMessage("Alarmbot received *DangerousBlocksSkew*")
      log.info("alarmbot received DANGEROUS BLOCK SKEW")
    /*
    case NewBlock(block) =>
      val response: Option[String] = sendMessage(
        "Alarmbot received blockid *%s*".format(Try(block.blockId).getOrElse(ByteVector32(ByteVector.empty))))
      log.info("message sent. response {}", response)
   */
  }
}