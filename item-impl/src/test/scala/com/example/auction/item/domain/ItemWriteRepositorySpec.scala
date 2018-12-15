package com.example.auction.item.domain

import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

import akka.persistence.query.Sequence
import com.datastax.driver.core.utils.UUIDs
import com.example.auction.item.impl._
import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.testkit.{ReadSideTestDriver, ServiceTest}
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}

class ItemWriteRepositorySpec extends AsyncWordSpec with BeforeAndAfterAll with Matchers {

  val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra()) { ctx =>
    new ItemApplication(ctx) {

      override def serviceLocator: ServiceLocator = NoServiceLocator

      override lazy val readSide: ReadSideTestDriver = new ReadSideTestDriver()

    }
  }

  override def afterAll(): Unit = server.stop()

  val testDriver = server.application.readSide
  //val itemRepository = server.application.
  val offset = new AtomicInteger()

  def sampleItem(creatorId: String): ItemAggregate = {
    ItemAggregate(
      UUIDs.timeBased().toString, creatorId, "title", "desc", "USD", 10, 100, None, ItemAggregateStatus.Created.toString, 10, None, None, None
    )
  }

  "The write repository" should {

    "create an item" in {
//      val creatorId= UUID.randomUUID().toString
//      val item = sampleItem(creatorId)
//      for {
//        _ <- feed(item.id, ItemCreated(item))
//        items <- getIt
//      }
      pending
    }

  }

  // Helpers -----------------------------------------------------------------------------------------------------------

  private def getItems(creatorId: String, itemStatus: String) = {
    //itemRep
    ???
  }

  private def feed(itemId: UUID, event: ItemEvent) = {
    testDriver.feed(itemId.toString, event, Sequence(offset.getAndIncrement))
  }

}