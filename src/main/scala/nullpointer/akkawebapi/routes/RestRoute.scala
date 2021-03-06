package nullpointer.akkawebapi.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import nullpointer.akkawebapi.json.EntityJsonFormat.RestEntityJsonFormat
import nullpointer.akkawebapi.Entities.RestEntity
import nullpointer.akkawebapi.ErrorResponses.ErrorResponse
import nullpointer.akkawebapi.repositories.Repositories.RestRepository
import spray.json._

import scala.reflect.ClassTag
import scala.util.{Failure, Success}

case class RestRoute[D](repository: RestRepository[D], pathPrefix: String)(implicit classTag: ClassTag[D], dataFormat: RootJsonFormat[D]) extends CustomRoute {
  private val className = classTag.runtimeClass.getSimpleName
  private implicit val entityFormat: RootJsonFormat[RestEntity[D]] = RestEntityJsonFormat[D]

  override def definition: Route = pathPrefix(pathPrefix) {
    pathPrefix(LongNumber) { id =>
      pathEndOrSingleSlash {
        get {
          onSuccess(repository.getById(id)) {
            case Some(data) => complete(data)
            case None => completeWithError(ErrorResponse(404, s"$className with requested id '$id' does not exist"))
          }
        } ~
        delete {
          onSuccess(repository.deleteById(id)) {
            case true => complete(StatusCodes.OK)
            case false => completeWithError(ErrorResponse(404, s"$className with requested id '$id' does not exist"))
          }
        }
      }
    } ~
    pathEndOrSingleSlash {
      get {
        complete(repository.getAll)
      } ~
      post {
        entity(as[D]) { data =>
          onSuccess(repository.add(data)) { addedData =>
            complete(StatusCodes.Created, addedData)
          }
        }
      } ~
      put {
        entity(as[RestEntity[D]]) { data =>
          onComplete(repository.update(data)) {
            case Success(_) => complete(StatusCodes.OK)
            case Failure(exception) => completeWithError(ErrorResponse(400, exception.getMessage))
          }
        }
      }
    }
  }
}
