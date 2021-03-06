package com.example.auction.transaction.domain

import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

import akka.Done
import akka.persistence.query.Sequence
import com.datastax.driver.core.utils.UUIDs
import com.example.auction.transaction.impl._
import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.testkit.{ReadSideTestDriver, ServiceTest}
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}

import scala.concurrent.Future

class TransactionRepositorySpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra()) { ctx =>
    new TransactionApplication(ctx) {
      override def serviceLocator: ServiceLocator = NoServiceLocator

      override lazy val readSide = new ReadSideTestDriver()
    }
  }

  override def afterAll(): Unit = server.stop()

  val testDriver = server.application.readSide
  val transactionRepository = server.application.transactionRepository
  val offset = new AtomicInteger()

  val itemId = UUIDs.timeBased()
  val creatorId = UUID.randomUUID
  val winnerId = UUID.randomUUID
  val itemTitle = "title"
  val currencyId = "EUR"
  val itemPrice = 2000
  val itemData = ItemData(itemTitle, "desc", currencyId, 1, 10, 10, None)
  val transaction = TransactionAggregate(itemId, creatorId, winnerId, itemData, itemPrice, None, None, None)

  val deliveryData = DeliveryData("Addr1", "Addr2", "City", "State", 27, "Country")
  val deliveryPrice = 500
  val payment = Payment("Payment sent via wire transfer")

  "The transaction repository" should {

    "get transaction started for creator" in {
      shouldGetTransactionStarted(creatorId)
    }

    "get transaction started for winner" in {
      shouldGetTransactionStarted(winnerId)
    }

    "update status to payment pending for creator" in {
      shouldUpdateStatusToPaymentPending(creatorId)
    }

    "update status to payment pending for winner" in {
      shouldUpdateStatusToPaymentPending(winnerId)
    }

    "update status to payment submitted for creator" in {
      shouldUpdateStatusToPaymentSubmitted(creatorId)
    }

    "update status to payment submitted for winner" in {
      shouldUpdateStatusToPaymentSubmitted(winnerId)
    }

    "update status to payment confirmed after approval for creator" in {
      shouldUpdateStatusToPaymentConfirmedAfterApproval(creatorId)
    }

    "update status to payment confirmed after approval for winner" in {
      shouldUpdateStatusToPaymentConfirmedAfterApproval(winnerId)
    }

    "update status to payment pending after rejection for creator" in {
      shouldUpdateStatusToPaymentPendingAfterRejection(creatorId)
    }

    "update status to payment pending after rejection for winner" in {
      shouldUpdateStatusToPaymentPendingAfterRejection(winnerId)
    }

    "paginate transaction retrieval" in {
      // TODO: Paging
      pending
    }
  }

  // Helpers -----------------------------------------------------------------------------------------------------------

  private def shouldGetTransactionStarted(userId: UUID) = {
    for {
      _ <- feed(TransactionStarted(itemId, transaction))
      transactions <- getTransactions(userId, "NegotiatingDelivery")
    } yield {
      transactions should have size 1
      val expected: TransactionSummary = TransactionSummary(itemId, creatorId, winnerId, itemTitle, currencyId, itemPrice, "NegotiatingDelivery")
      transactions.head should ===(expected)
    }
  }

  // TODO: Paging
  // TODO: Status enum
  private def getTransactions(userId: UUID, transactionStatus: String): Future[Seq[TransactionSummary]] = {
    transactionRepository.selectUserTransactions(userId, transactionStatus, 10)
  }

  private def feed(event: TransactionEvent): Future[Done] = {
    // TODO: Event needs itemId
    testDriver.feed(event.itemId.toString, event, Sequence(offset.getAndIncrement))
  }

  private def shouldUpdateStatusToPaymentPending(userId: UUID) = {
    for {
      _ <- feed(TransactionStarted(itemId, transaction))
      _ <- feed(DeliveryDetailsSubmitted(itemId, deliveryData))
      _ <- feed(DeliveryPriceUpdated(itemId, deliveryPrice))
      _ <- feed(DeliveryDetailsApproved(itemId))
      transactions <- getTransactions(userId, "PaymentPending")
    } yield {
      transactions should have size 1
      val expected = TransactionSummary(itemId, creatorId, winnerId, itemTitle, currencyId, itemPrice, "PaymentPending")
      transactions.head should ===(expected)
    }
  }

  private def shouldUpdateStatusToPaymentSubmitted(userId: UUID) = {
    for {
      _ <- feed(TransactionStarted(itemId, transaction))
      _ <- feed(DeliveryDetailsSubmitted(itemId, deliveryData))
      _ <- feed(DeliveryPriceUpdated(itemId, deliveryPrice))
      _ <- feed(DeliveryDetailsApproved(itemId))
      _ <- feed(PaymentDetailsSubmitted(itemId, payment))
      transactions <- getTransactions(userId, "PaymentSubmitted")
    } yield {
      transactions should have size 1
      val expected = TransactionSummary(itemId, creatorId, winnerId, itemTitle, currencyId, itemPrice, "PaymentSubmitted")
      transactions.head should ===(expected)
    }
  }

  private def shouldUpdateStatusToPaymentConfirmedAfterApproval(userId: UUID) = {
    for {
      _ <- feed(TransactionStarted(itemId, transaction))
      _ <- feed(DeliveryDetailsSubmitted(itemId, deliveryData))
      _ <- feed(DeliveryPriceUpdated(itemId, deliveryPrice))
      _ <- feed(DeliveryDetailsApproved(itemId))
      _ <- feed(PaymentDetailsSubmitted(itemId, payment))
      _ <- feed(PaymentApproved(itemId))
      transactions <- getTransactions(userId, "PaymentConfirmed")
    } yield {
      transactions should have size 1
      val expected = TransactionSummary(itemId, creatorId, winnerId, itemTitle, currencyId, itemPrice, "PaymentConfirmed")
      transactions.head should ===(expected)
    }
  }

  private def shouldUpdateStatusToPaymentPendingAfterRejection(userId: UUID) = {
    for {
      _ <- feed(TransactionStarted(itemId, transaction))
      _ <- feed(DeliveryDetailsSubmitted(itemId, deliveryData))
      _ <- feed(DeliveryPriceUpdated(itemId, deliveryPrice))
      _ <- feed(DeliveryDetailsApproved(itemId))
      _ <- feed(PaymentDetailsSubmitted(itemId, payment))
      _ <- feed(PaymentRejected(itemId))
      transactions <- getTransactions(userId, "PaymentPending")
    } yield {
      transactions should have size 1
      val expected = TransactionSummary(itemId, creatorId, winnerId, itemTitle, currencyId, itemPrice, "PaymentPending")
      transactions.head should ===(expected)
    }
  }

}
