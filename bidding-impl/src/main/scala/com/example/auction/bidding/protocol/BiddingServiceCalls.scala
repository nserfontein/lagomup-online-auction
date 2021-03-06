package com.example.auction.bidding.protocol

import java.util.UUID

import akka.NotUsed
import com.example.auction.bidding.api._
import com.example.auction.bidding.impl
import com.example.auction.bidding.impl.{Bid => _, PlaceBid => _, _}
import com.example.auction.utils.ServerSecurity
import com.lightbend.lagom.scaladsl.server.ServerServiceCall

import scala.concurrent.{ExecutionContext, Future}

trait BiddingServiceCalls {

  val ports: BiddingPorts
  implicit val serviceEC: ExecutionContext = ports.akkaComponents.ec

  def _placeBid(userId: UUID, itemId: UUID, request: PlaceBid): Future[BidResult] = {
    ports.entityRegistry.refFor[BiddingEntity](itemId.toString).ask(impl.PlaceBid(request.maximumBidPrice, userId)).map { result =>
      // TODO: result.status enum
      BidResult(result.currentPrice, result.status, result.currentBidder)
    }
  }

  def _getBids(itemId: UUID, request: NotUsed): Future[List[Bid]] = {
    ports.entityRegistry.refFor[BiddingEntity](itemId.toString).ask(GetState).map {
      case BiddingState(Some(aggregate), _) => aggregate.biddingHistory.map(convertBid).reverse
      case BiddingState(None, _) => List.empty
    }
  }

  def _authenticatePlaceBid[Request, Response](serviceCall: UUID => ServerServiceCall[Request, Response]): ServerServiceCall[Request, Response] = {
    ServerSecurity.authenticated(serviceCall)
  }

  // -------------------------------------------------------------------------------------------------------------------

  private def convertBid(bid: impl.Bid): Bid = Bid(bid.bidder, bid.bidTime, bid.bidPrice, bid.maximumBid)

}
