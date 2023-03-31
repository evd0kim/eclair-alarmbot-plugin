package fr.acinq.alarmbot

import sttp.client3.{Response, SttpBackend, UriContext, asString, basicRequest}
import sttp.model.Uri
import fr.acinq.eclair.MilliSatoshi
import sttp.client3.json4s.asJson

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import org.json4s.jackson.Serialization.write

class KolliderClient(pluginConfig: AlarmBotConfig) {
  val botApiKey = pluginConfig.botApiKey
  val chatId = pluginConfig.chatId
  val baseUri: Uri = uri"https://api.telegram.org/bot$botApiKey/sendMessage"

  val readTimeout: FiniteDuration = 10.seconds

  def checkAvailability(host: String)(implicit sttpBackend: SttpBackend[Future, _], ec: ExecutionContext): Future[Response[String]] = {
      basicRequest.get(uri"$host/ping")
        .response(asString.getRight)
        .send(sttpBackend).map(identity)
  }

  case class HedgeResponse()
  case class HedgeRequest(channel: String, local: Long, remote: Long, ticker: String, amount: Long, rate: Long)

  def addPosition(channel: String, localUpdates: Long, remoteUpdates: Long, ticker: String, amount: MilliSatoshi, rate: MilliSatoshi)(implicit sttpBackend: SttpBackend[Future, _], ec: ExecutionContext): Future[Response[HedgeResponse]] = {
    implicit val serialization = org.json4s.native.Serialization
    implicit val formats = org.json4s.DefaultFormats

    val htlcApiUri: Uri = uri"${pluginConfig.hedgeServicesMap.apply(ticker)}/hedge"

    val hedgeRequest = HedgeRequest(
      channel,
      localUpdates,
      remoteUpdates,
      ticker.toUpperCase(),
      amount.truncateToSatoshi.toLong,
      rate.truncateToSatoshi.toLong)

    //println(s"Channel $channel ticker $ticker $htlcApiUri ${write(hedgeRequest)}")

    basicRequest.post(htlcApiUri)
      .contentType("application/json")
      .body(write(hedgeRequest))
      .response(asJson[HedgeResponse].getRight)
      .send(sttpBackend)
  }

  def sendMessage(amount: MilliSatoshi, rate: MilliSatoshi)(implicit sttpBackend: SttpBackend[Future, Nothing], ec: ExecutionContext): Future[Response[String]] = {
    val parametrizedUri = baseUri.addParams("chat_id" -> chatId, "text" -> s"hedging ${amount} ${rate}", "parse_mode" -> "HTML")
    basicRequest.get(parametrizedUri)
      .response(asString.getRight)
      .send(sttpBackend)
  }
}