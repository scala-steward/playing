package elasticsearch

import blog.Posts
import com.sksamuel.elastic4s.ElasticClient
import play.api.libs.json.{Json, Writes}
import scala.concurrent.ExecutionContext.Implicits.global
import play.Logger

/**
 * Created by bharadwaj on 10/04/14.
 */
trait BlogElasticSearch extends Posts {

  Logger.info("going to connect to bonsai host = " + bonsaiHost)
  //val client = ElasticClient.local
  val client = ElasticClient.remote(bonsaiHost, 9300)

  case class Search(title: String, url: String, tags: String, category: String,
                    date: String, content: String, score: Float, fragments: Seq[String])

  case class Searches(s: Seq[Search])

  implicit val searchWrites = new Writes[Search] {
    def writes(s: Search) = Json.obj(
      titleField       -> s.title,
      urlField         -> s.url,
      tagsField        -> s.tags,
      categoryField    -> s.category,
      dateField        -> s.date,
      contentField     -> s.content,
      scoreField       -> s.score,
      fragmentsField   -> s.fragments
    )
  }

  implicit val searchesWrites = new Writes[Searches] {
    def writes(searches: Searches) = Json.obj(
      searchField -> searches.s
    )
  }
}
