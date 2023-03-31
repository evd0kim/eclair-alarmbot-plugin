package fr.acinq.alarmbot

import fr.acinq.eclair.MilliSatoshi
import org.json4s.jackson.Serialization.write
import sttp.client3.json4s._
import sttp.client3._
import sttp.model.Uri

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class KolliderClient(pluginConfig: AlarmBotConfig) {
  val botApiKey = pluginConfig.botApiKey
  val chatId = pluginConfig.chatId
  val baseUri: Uri = uri"https://api.telegram.org/bot$botApiKey/sendMessage"

  val serviceUri: Uri = uri"${pluginConfig.hedgeService.getOrElse(throw new RuntimeException("config.hedgeServiceUri is not set"))}"

  val readTimeout: FiniteDuration = 10.seconds

  def checkAvailability()(implicit http: SttpBackend[Future, _], ec: ExecutionContext): Future[Response[Either[String, String]]] = {
    val parametrizedUri = serviceUri.path("/state")
    val request = basicRequest.get(parametrizedUri)
    http.send(request)
  }

  case class HedgeResponse()
  case class HedgeRequest(channel_id: String, sats: Long, rate: Long)

  def addPosition(channel: String, amount: MilliSatoshi, rate: MilliSatoshi)(implicit http: SttpBackend[Future, _], ec: ExecutionContext): Future[Response[Either[ResponseException[String, Exception], HedgeResponse]]] = {
    implicit val serialization = org.json4s.jackson.Serialization
    implicit val formats = org.json4s.DefaultFormats

    val htlcApiUri = serviceUri.path("/hedge/htlc")

    val hedgeRequest = HedgeRequest(
      channel,
      amount.truncateToSatoshi.toLong,
      rate.truncateToSatoshi.toLong)

    val request = basicRequest.readTimeout(readTimeout)
      .contentType("application/json")
      .post(htlcApiUri)
      .body(write(hedgeRequest))
      .response(asJson[HedgeResponse])

    http.send(request)
  }

  def sendMessage(amount: MilliSatoshi, rate: MilliSatoshi)(implicit http: SttpBackend[Future, _], ec: ExecutionContext): Future[Response[Either[String, String]]] = {
    val parametrizedUri = baseUri.params("chat_id" -> chatId, "text" -> s"hedging ${amount} ${rate}", "parse_mode" -> "HTML")
    val request = basicRequest.readTimeout(readTimeout).get(parametrizedUri)
    http.send(request)
  }
}