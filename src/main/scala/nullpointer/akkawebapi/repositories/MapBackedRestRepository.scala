package nullpointer.akkawebapi.repositories

import java.util.concurrent.atomic.AtomicLong

import nullpointer.akkawebapi.models.Entities.Entity
import nullpointer.akkawebapi.repositories.Repositories.RestRepository

import scala.collection.concurrent.TrieMap
import scala.concurrent.{ExecutionContext, Future}

class MapBackedRestRepository[D](implicit ec: ExecutionContext) extends RestRepository[D] {
  private val items: collection.concurrent.Map[Long, D] = TrieMap()
  private val idCounter: AtomicLong = new AtomicLong(0)

  override def getById(id: Long): Future[Option[RepositoryEntity]] = Future(items.get(id).map(d => Entity(Some(id), d)))

  override def getAll: Future[Seq[RepositoryEntity]] = Future(items.map(p => Entity(Some(p._1), p._2)).toSeq)

  override def add(data: D): Future[RepositoryEntity] = Future {
    val dataId = idCounter.incrementAndGet()
    items.putIfAbsent(dataId, data) match {
      case Some(_) => null
      case None => Entity(Some(dataId), data)
    }
  }

  override def update(entity: RepositoryEntity): Future[Unit] = Future {
    entity.id match {
      case Some(id) => items.put(id, entity.data)
      case None =>
    }
  }

  override def deleteById(id: Long): Future[Boolean] = Future {
    items.remove(id).isDefined
  }
}