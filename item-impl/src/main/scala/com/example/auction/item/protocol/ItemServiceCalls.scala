package com.example.auction.item.protocol


import akka.persistence.cassandra.ListenableFutureConverter
import akka.{Done, NotUsed}
import com.datastax.driver.core.utils.UUIDs
import com.datastax.driver.core.{PagingState, SimpleStatement, _}
import com.example.auction.item.api._
import com.example.auction.item.impl._
import com.lightbend.lagom.scaladsl.api.transport.Forbidden
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import com.lightbend.lagom.scaladsl.pubsub.PubSubRegistry
import com.lightbend.lagom.scaladsl.server.ServerServiceCall

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}


trait ItemServiceCalls {
  implicit val ec: ExecutionContext

  val entityRegistry: PersistentEntityRegistry
  val db: CassandraSession
  val pubSubRegistry: PubSubRegistry

  def _createItem(userId: String, request: Item): Future[Item] = {
    if (userId != request.creator) {
      throw Forbidden("User " + userId + " can't created an item on behalf of " + request.creator)
    }
    val itemId = UUIDs.timeBased().toString // TODO: UUID
    val pItem = ItemAggregate(
      id = itemId,
      creator = request.creator,
      title = request.itemData.title,
      description = request.itemData.description,
      currencyId = request.itemData.currencyId,
      increment = request.itemData.increment,
      reservePrice = request.itemData.reservePrice,
      price = None,
      status = CreatedStatus,
      auctionDuration = request.itemData.auctionDuration,
      auctionStart = None,
      auctionEnd = None,
      auctionWinner = None
    )
    entityRegistry.refFor[ItemEntity](itemId).ask(CreateItem(pItem)).map { _ =>
      convertItem(pItem)
    }
  }

  def _startAuction(id: String, userId: String, request: NotUsed): Future[Done] = {
    entityRegistry.refFor[ItemEntity](id).ask(StartAuction(userId))
  }

  def _getItem(id: String, request: NotUsed): Future[Item] = {
    // TODO: Example tested for None here
    entityRegistry.refFor[ItemEntity](id).ask(GetItem).map(convertItem)
  }

  def _getItemForUser(id: String, status: String, page: Option[String], request: NotUsed): Future[ItemSummaryPagingState] = {
    getItemsForUser(id, status, page, DefaultFetchSize)
  }

  def _createItemAuthentication[Request, Response](serviceCall: String => ServerServiceCall[Request, Response]): ServerServiceCall[Request, Response] = {
    ServerSecurity.authenticated(serviceCall)
  }

  def _startAuctionAuthentication[Request, Response](serviceCall: String => ServerServiceCall[Request, Response]): ServerServiceCall[Request, Response] = {
    ServerSecurity.authenticated(serviceCall)
  }

  // -------------------------------------------------------------------------------------------------------------------

  val DefaultFetchSize = 10

  val CreatedStatus = "Created"

  // Table
  val ItemEventOffset = "itemEventOffset"
  val ItemCreatorTable = "itemCreator"
  val ItemSummaryByCreatorTable = "itemSummaryByCreator"
  val ItemSummaryByCreatorAndStatusMV = "itemSummaryByCreatorAndStatus"

  // Fields
  val ItemId = "itemId"
  val CreatorId = "creatorId"
  val Title = "title"
  val CurrencyId = "currencyId"
  val ReservePrice = "reservePrice"
  val Status = "status"

  def convertItem(item: ItemAggregate) = {
    val itemData = ItemData(
      item.title, item.description, item.currencyId, item.increment, item.reservePrice, item.auctionDuration, None
    )
    Item(
      Some(item.id), item.creator, itemData, item.price, item.status, item.auctionStart, item.auctionEnd, item.auctionWinner
    )
  }

  private def getItemsForUser(creatorId: String, status: String, page: Option[String], fetchSize: Int): Future[ItemSummaryPagingState] = {
    for {
      count <- countItemsByCreatorInStatus(creatorId, status)
      itemsWithNextPage <- selectItemsByCreatorInStatusWithPaging(creatorId, status, page, fetchSize)
    } yield {
      val items = itemsWithNextPage._1
      val nextPage = itemsWithNextPage._2.getOrElse("")
      ItemSummaryPagingState(items.toList, nextPage, count)
    }

  }

  private def countItemsByCreatorInStatus(creatorId: String, status: String) = {
    db.selectOne(
      s"""
      SELECT COUNT(*)
      FROM $ItemSummaryByCreatorAndStatusMV
      WHERE $CreatorId = ? AND $Status = ?
      ORDER BY $Status ASC, $ItemId DESC
    """, // ORDER BY status is required due to https://issues.apache.org/jira/browse/CASSANDRA-10271
      creatorId, status.toString).map {
      case Some(row) => row.getLong("count").toInt
      case None => 0
    }
  }

  private def convertItemSummary(item: Row): ItemSummary = {
    ItemSummary(
      item.getString(ItemId), // TODO: UUID
      item.getString(Title),
      item.getString(CurrencyId),
      item.getInt(ReservePrice),
      item.getString(Status)
    )
  }

  /**
    * Motivation: https://discuss.lightbend.com/t/how-to-specify-pagination-for-select-query-read-side/870
    */
  private def selectItemsByCreatorInStatusWithPaging(creatorId: String,
                                                     status: String,
                                                     page: Option[String],
                                                     fetchSize: Int): Future[(Seq[ItemSummary], Option[String])] = {
    val statement = new SimpleStatement(
      s"""
      SELECT *
      FROM $ItemSummaryByCreatorAndStatusMV
      WHERE $CreatorId = ? AND $Status = ?
      ORDER BY $Status ASC, $ItemId DESC
      """, creatorId, status.toString)

    statement.setFetchSize(fetchSize)

    db.underlying().flatMap(underlyingSession => {

      page.map(pagingStateStr => statement.setPagingState(PagingState.fromString(pagingStateStr)))

      underlyingSession.executeAsync(statement).asScala.map(resultSet => {
        val newPagingState = resultSet.getExecutionInfo.getPagingState

        /**
          * @note Check against null due to Java code in `getPagingState` function.
          * @note The `getPagingState` function can return null if there is no next page for this reason nextPage is an
          *       Option[String].
          */
        val nextPage: Option[String] = if (newPagingState != null) Some(newPagingState.toString) else None

        val iterator = resultSet.iterator().asScala
        iterator.take(fetchSize).map(convertItemSummary).toSeq -> nextPage
      })

    })
  }

}

