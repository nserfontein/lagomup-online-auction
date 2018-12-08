package com.example.auction.bidding.impl


import com.example.auction.bidding.api._
import com.example.auction.bidding.protocol._
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.pubsub.PubSubRegistry
import akka.stream.scaladsl.Flow
import scala.concurrent.ExecutionContext
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession


class BiddingServiceImpl(val entityRegistry: PersistentEntityRegistry, val db: CassandraSession, val pubSubRegistry: PubSubRegistry)
                                         (implicit val ec: ExecutionContext)  extends BiddingService
  with BiddingServiceCalls with BiddingTopics  {

  
  override def placeBid() = ServiceCall { request =>
  _placeBid(request)
}

override def getBids() = ServiceCall { request =>
  _getBids(request)
}


  override def bidEvents() = {
  _bidEvents()
}



}

