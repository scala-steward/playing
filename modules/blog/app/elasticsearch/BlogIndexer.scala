package elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import scala.collection.immutable.HashSet
import play.api.Logger
import play.api.libs.json.{Writes, Json}
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse
import scala.async.Async.{async, await}
import scala.concurrent.{Await, Future, ExecutionContext}
import scala.concurrent.duration.{FiniteDuration, DurationInt}
import scala.concurrent.Future
import org.elasticsearch.action.index.IndexResponse
import scala.language.postfixOps

/**
 * Created by bharadwaj on 09/04/14.
 */
object BlogIndexer extends BlogElasticSearch {

  val searches = setupIndex

  def setupIndex: Searches = {
    var searches = scala.collection.mutable.Seq[Search]()
    for (file <- posts) {
      val lines = fileContent("public/posts/" + file)
      val header = lines.takeWhile(line => !line.equals("}}}")).toSeq
      val title = getLine(header, "\"title\"")
      val date = getLine(header, "\"date\"")

      //val subheading = getLine(header, "\"subheading\"")
      val category = getLine(header, "\"category\"")
      //val description = getLine(header, "\"description\"")
      val tagLine = getLine(header, "\"tags\"").filter(!"[]\"".contains(_))

      val tags = tagLine.split(" ")
      var tagSet = new HashSet[String]
      for {tag <- tags; if (tag.length > 0)} {
        //println("tag = " + tag)
        tagSet += tag.trim
      }

      val content: String = lines.dropWhile(line => !line.equals("}}}")).drop(1).mkString(" ")
      val search = Search(title, "post/" + file.replaceAll(".md", ""), tagLine, category, date, content, 0, Seq())
      searches = searches :+ search
    }
    Searches(searches)
  }

  val dIndex = {
    Logger.info("Starting Index Delete...")
    client.execute {
      deleteIndex(blogIndex)
    }
  }
  val cIndex = {
    Logger.info("Starting Index Create...")
    client.execute {
      create.index(blogIndex)
    }
  }

  def createIndex = {
    Await.ready(dIndex, 1 minute)
    Logger.info("Finished Index Delete.")

    Await.ready(cIndex, 1 minute)
    Logger.info("Finished Index Creation.")

    // convert the sequence of futures to => future of a sequence, that is
    // Seq[Future[IndexResponse]] => Future[Seq[IndexResponse]]
    // And finally wait on that single future to complete
    val f = Future.sequence(insertBlog)
    Await.ready(f, 1 minute)
    Logger.info("Finished document insertions.")
  }

  def getOrNull(s: String): String = if(s.length > 0) s else "none"

  val insertBlog: Seq[Future[IndexResponse]] =
    for (s <- searches.s)
    yield client.execute {
      index into blogIndex + "/" + postType fields(
        titleField    -> getOrNull(s.title),
        urlField      -> getOrNull(s.url),
        tagsField     -> getOrNull(s.tags),
        categoryField -> getOrNull(s.category),
        dateField     -> getOrNull(s.date),
        contentField  -> getOrNull(s.content)
        )
    }

  def shutdown = client.close()
}
