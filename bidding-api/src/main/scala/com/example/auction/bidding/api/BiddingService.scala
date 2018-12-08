package com.example.auction.bidding.api


import akka.{Done, NotUsed}
import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import akka.util.ByteString
import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import com.lightbend.lagom.scaladsl.playjson.JsonSerializer
import julienrf.json.derived
import play.api.libs.json._
import java.time.Instant

trait BiddingService extends Service {
  def placeBid(): ServiceCall[PlaceBid, BidResult]

def getBids(): ServiceCall[NotUsed, List[Bid]]


  
  override def descriptor = {
  import Service._

  named("bidding")
    .withCalls(
  pathCall("/api/item/:id/bids", placeBid _)(implicitly[MessageSerializer[PlaceBid, ByteString]], implicitly[MessageSerializer[BidResult, ByteString]]),
pathCall("/api/item/:id/bids", getBids _)(implicitly[MessageSerializer[NotUsed, ByteString]], implicitly[MessageSerializer[List[Bid], ByteString]])
)

    
    .withAutoAcl(true)
}

      
}

case class PlaceBid(maximumBidPrice: Int) 

object PlaceBid {
  implicit val format: Format[PlaceBid] = Json.format
}

case class BidResult(currentPrice: Int, status: String, currentBidder: Option[String]) 

object BidResult {
  implicit val format: Format[BidResult] = Json.format
}

case class Bid(bidder: String, bidTime: Instant, price: Int, maximumPrice: Int) 

object Bid {
  implicit val format: Format[Bid] = Json.format
}

