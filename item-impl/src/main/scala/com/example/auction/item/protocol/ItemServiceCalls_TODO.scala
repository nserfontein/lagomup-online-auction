package com.example.auction.item.protocol

import com.example.auction.bidding.api._

import com.example.auction.item.api._
import com.example.auction.item.impl.ItemPorts
import akka.stream.scaladsl.Source
import akka.{ Done, NotUsed }
import akka.actor.ActorSystem
import akka.stream.Materializer
import com.lightbend.lagom.scaladsl.pubsub.{ PubSubRegistry, TopicId }
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import scala.concurrent.{ ExecutionContext, Future }
import java.util.UUID

trait ItemServiceCalls_TODO {

  val ports: ItemPorts

  def _createItem(userId: UUID, request: Item): Future[Item] = {
    ???
  }

  def _startAuction(userId: UUID, id: UUID, request: NotUsed): Future[Done] = {
    ???
  }

  def _getItem(id: UUID, request: NotUsed): Future[Item] = {
    ???
  }

  def _getItemsForUser(id: UUID, status: String, page: Option[String], request: NotUsed): Future[ItemSummaryPagingState] = {
    ???
  }

  def _authenticateCreateItem[Request, Response](serviceCall: UUID => ServerServiceCall[Request, Response]): ServerServiceCall[Request, Response] = {
    ???
  }

  def _authenticateStartAuction[Request, Response](serviceCall: UUID => ServerServiceCall[Request, Response]): ServerServiceCall[Request, Response] = {
    ???
  }

}

