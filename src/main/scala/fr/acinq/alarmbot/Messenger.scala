package fr.acinq.alarmbot

import sttp.client3.{asString, Response, SttpBackend, UriContext, basicRequest}
import sttp.model.Uri

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class Messenger(pluginConfig: AlarmBotConfig) {
  val botApiKey = pluginConfig.botApiKey
  val chatId = pluginConfig.chatId
  val baseUri: Uri = uri"https://api.telegram.org/bot$botApiKey/sendMessage"

  def sendMessage(message: String)(implicit sttpBackend: SttpBackend[Future, _], ec: ExecutionContext): Future[Response[String]] = {
    val parametrizedUri = baseUri.addParams("chat_id" -> chatId, "text" -> message, "parse_mode" -> "HTML")
    basicRequest.get(parametrizedUri)
      .response(asString.getRight)
      .send(sttpBackend)
  }

}