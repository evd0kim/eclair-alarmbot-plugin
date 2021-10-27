package fr.acinq.alarmbot

import com.softwaremill.sttp._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class Messenger(pluginConfig: AlarmBotConfig) {
  val botApiKey = pluginConfig.botApiKey
  val chatId = pluginConfig.chatId
  val baseUri: Uri = uri"https://api.telegram.org/bot$botApiKey/sendMessage"

  val readTimeout: FiniteDuration = 10.seconds

  def sendMessage(message: String)(implicit http: SttpBackend[Future, Nothing], ec: ExecutionContext): Future[Response[String]] = {
    val parametrizedUri = baseUri.params("chat_id" -> chatId, "text" -> message, "parse_mode" -> "HTML")
    sttp.readTimeout(readTimeout).get(parametrizedUri).send.map(identity)
  }
}