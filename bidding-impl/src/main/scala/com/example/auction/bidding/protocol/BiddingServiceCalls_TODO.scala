package com.example.auction.bidding.protocol


import com.example.auction.bidding.api._
import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.lightbend.lagom.scaladsl.pubsub.{PubSubRegistry, TopicId}
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import scala.concurrent.{ExecutionContext, Future}

trait BiddingServiceCalls_TODO {
  implicit val ec: ExecutionContext
  implicit val mat: Materializer

  
  val entityRegistry: PersistentEntityRegistry
val db: CassandraSession
val pubSubRegistry: PubSubRegistry
val actorSystem: ActorSystem



  def _placeBid(itemId: String, userId: String, request: PlaceBid): Future[BidResult] = {
  ???
}

def _getBids(itemId: String, request: NotUsed): Future[List[Bid]] = {
  ???
}



  def _placeBidAuthentication[Request, Response](serviceCall: String => ServerServiceCall[Request, Response]): ServerServiceCall[Request, Response] = {
  ???
}



}

