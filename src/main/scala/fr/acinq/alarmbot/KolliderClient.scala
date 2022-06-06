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

  //val serviceUri: Uri = uri"${pluginConfig.hedgeServices.getString("BTCEUR")}"

  val readTimeout: FiniteDuration = 10.seconds

  def checkAvailability()(implicit sttpBackend: SttpBackend[Future, _], ec: ExecutionContext): Future[Response[String]] = {
    val parametrizedUri: Uri = uri"${pluginConfig.hedgeServicesMap.get("BTCEUR")}" //serviceUri.addPath("state")
    basicRequest.get(parametrizedUri)
      .response(asString.getRight)
      .send(sttpBackend).map(identity)
  }

  case class HedgeResponse()
  case class HedgeRequest(channel_id: String, sats: Long, rate: Long)

  def addPosition(channel: String, amount: MilliSatoshi, rate: MilliSatoshi)(implicit sttpBackend: SttpBackend[Future, _], ec: ExecutionContext): Future[Response[HedgeResponse]] = {
    implicit val serialization = org.json4s.native.Serialization
    implicit val formats = org.json4s.DefaultFormats

    val htlcApiUri: Uri = uri"${pluginConfig.hedgeServicesMap.get("BTCEUR")}/hedge/htlc"
    //val htlcApiUri = serviceUri.addPath("hedge", "htlc")

    val hedgeRequest = HedgeRequest(
      channel,
      amount.truncateToSatoshi.toLong,
      rate.truncateToSatoshi.toLong)

    println(write(hedgeRequest))

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