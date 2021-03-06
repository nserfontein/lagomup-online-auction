package com.example.auction.transaction.protocol

import java.util.UUID

import akka.{Done, NotUsed}
import com.example.auction.item.api
import com.example.auction.transaction.api._
import com.example.auction.transaction.impl.{TransactionSummary => _, _}
import com.example.auction.utils.ServerSecurity
import com.lightbend.lagom.scaladsl.api.transport.{Forbidden, NotFound}
import com.lightbend.lagom.scaladsl.server.ServerServiceCall

import scala.concurrent.Future

trait TransactionServiceCalls {

  val ports: TransactionPorts
  implicit val ec = ports.akkaComponents.ec

  def _submitDeliveryDetails(userId: UUID, itemId: UUID, request: DeliveryInfo): Future[Done] = {
    ports.entityRegistry.refFor[TransactionEntity](itemId.toString)
      .ask(SubmitDeliveryDetails(userId, fromApi(request)))
  }

  def _setDeliveryPrice(userId: UUID, itemId: UUID, request: Int): Future[Done] = {
    ports.entityRegistry.refFor[TransactionEntity](itemId.toString)
      .ask(SetDeliveryPrice(userId, request))
  }

  def _approveDeliveryDetails(userId: UUID, itemId: UUID, request: NotUsed): Future[Done] = {
    ports.entityRegistry.refFor[TransactionEntity](itemId.toString)
      .ask(ApproveDeliveryDetails(userId))
  }

  def _submitPaymentDetails(userId: UUID, itemId: UUID, request: PaymentInfo): Future[Done] = {
    ports.entityRegistry.refFor[TransactionEntity](itemId.toString)
      .ask(SubmitPaymentDetails(userId, fromApi(request)))
  }

  def _submitPaymentStatus(userId: UUID, itemId: UUID, request: String): Future[Done] = {
    ports.entityRegistry.refFor[TransactionEntity](itemId.toString)
      .ask(SubmitPaymentStatus(userId, request))
  }

  def _getTransaction(userId: UUID, itemId: UUID, request: NotUsed): Future[TransactionInfo] = {
    ports.entityRegistry.refFor[TransactionEntity](itemId.toString)
      .ask(GetState)
      .map {
        case TransactionState(None, _) =>
          throw NotFound(s"Transaction for item $itemId not found")
        case TransactionState(Some(aggregate), status) =>
          if (userId == aggregate.creator || userId == aggregate.winner) {
            toApi(aggregate, status)
          } else {
            throw Forbidden("Only the item owner and the auction winner can see transaction details")
          }
      }
  }

  def _getTransactionsForUser(userId: UUID, status: String, pageNo: Option[String], pageSize: Option[String], request: NotUsed): Future[List[TransactionSummary]] = {
    ???
  }

  // Authentication ----------------------------------------------------------------------------------------------------

  def _authenticateSubmitDeliveryDetails[Request, Response](serviceCall: UUID => ServerServiceCall[Request, Response]): ServerServiceCall[Request, Response] = {
    ServerSecurity.authenticated(serviceCall)
  }

  def _authenticateSetDeliveryPrice[Request, Response](serviceCall: UUID => ServerServiceCall[Request, Response]): ServerServiceCall[Request, Response] = {
    ServerSecurity.authenticated(serviceCall)
  }

  def _authenticateApproveDeliveryDetails[Request, Response](serviceCall: UUID => ServerServiceCall[Request, Response]): ServerServiceCall[Request, Response] = {
    ServerSecurity.authenticated(serviceCall)
  }

  def _authenticateSubmitPaymentDetails[Request, Response](serviceCall: UUID => ServerServiceCall[Request, Response]): ServerServiceCall[Request, Response] = {
    ServerSecurity.authenticated(serviceCall)
  }

  def _authenticateSubmitPaymentStatus[Request, Response](serviceCall: UUID => ServerServiceCall[Request, Response]): ServerServiceCall[Request, Response] = {
    ServerSecurity.authenticated(serviceCall)
  }

  def _authenticateGetTransaction[Request, Response](serviceCall: UUID => ServerServiceCall[Request, Response]): ServerServiceCall[Request, Response] = {
    ServerSecurity.authenticated(serviceCall)
  }

  def _authenticateGetTransactionsForUser[Request, Response](serviceCall: UUID => ServerServiceCall[Request, Response]): ServerServiceCall[Request, Response] = {
    ServerSecurity.authenticated(serviceCall)
  }

  // Mappers -----------------------------------------------------------------------------------------------------------

  private def toApi(aggregate: TransactionAggregate, status: TransactionAggregateStatus.Status): TransactionInfo = {
    TransactionInfo(
      aggregate.itemId,
      aggregate.creator,
      aggregate.winner,
      toApi(aggregate.itemData),
      aggregate.itemPrice,
      aggregate.deliveryData.map(toApi),
      aggregate.deliveryPrice,
      aggregate.payment.map(toApi),
      status.toString
    )
  }

  private def toApi(deliveryData: DeliveryData): DeliveryInfo = {
    DeliveryInfo(
      deliveryData.addressLine1,
      deliveryData.addressLine2,
      deliveryData.city,
      deliveryData.state,
      deliveryData.postalCode,
      deliveryData.country
    )
  }

  private def toApi(payment: Payment): PaymentInfo = {
    PaymentInfo(payment.comment)
  }

  private def toApi(itemData: ItemData): api.ItemData = {
    api.ItemData(
      itemData.title,
      itemData.description,
      itemData.currencyId,
      itemData.increment,
      itemData.reservePrice,
      itemData.auctionDuration,
      itemData.categoryId
    )
  }

  private def fromApi(deliveryInfo: DeliveryInfo): DeliveryData = {
    DeliveryData(
      deliveryInfo.addressLine1,
      deliveryInfo.addressLine2,
      deliveryInfo.city,
      deliveryInfo.state,
      deliveryInfo.postalCode,
      deliveryInfo.country
    )
  }

  private def fromApi(paymentInfo: PaymentInfo): Payment = {
    Payment(paymentInfo.comment)
  }

}

