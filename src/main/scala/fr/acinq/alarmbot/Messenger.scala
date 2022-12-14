package fr.acinq.alarmbot

import sttp.client3._
import sttp.model.Uri

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class Messenger(pluginConfig: AlarmBotConfig) {
  val botApiKey = pluginConfig.botApiKey
  val chatId = pluginConfig.chatId
  val baseUri: Uri = uri"https://api.telegram.org/bot$botApiKey/sendMessage"

  val readTimeout: FiniteDuration = 10.seconds

  def sendMessage(message: String)(implicit http: SttpBackend[Future, _], ec: ExecutionContext): Future[Response[Either[String, String]]] = {
    val parametrizedUri = baseUri.params("chat_id" -> chatId, "text" -> message, "parse_mode" -> "HTML")
    val request = basicRequest.readTimeout(readTimeout).get(parametrizedUri)
    http.send(request)
  }
}