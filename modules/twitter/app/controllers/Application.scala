package controllers.twitter

import play.api.mvc._
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.{HttpPost, HttpGet}
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer
import org.apache.commons.io.IOUtils
import play.api._
import org.apache.http.HttpResponse
import org.apache.http.message.BasicNameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import scala.collection.JavaConversions
import twitter._
import akka.actor.{ActorRef, Props, ActorSystem}
import play.api.libs.iteratee.Enumerator
import java.io.ByteArrayInputStream
import models.twitter._
import twitter.TweetProcessor
import twitter.TweetStreamerActor
import twitter.Credentials

import scala.concurrent._
import ExecutionContext.Implicits.global
import play.api.libs.json.JsValue
import scala.concurrent.duration.{FiniteDuration, DurationInt}

/**
 * Created by bharadwaj on 27/01/14.
 */
object Application extends Controller {

  val client = new DefaultHttpClient()
  val consumer = new CommonsHttpOAuthConsumer(Credentials.ck, Credentials.cs)
  consumer.setTokenWithSecret(Credentials.at, Credentials.as)

  var processors: Map[String, ActorRef] = Map()
  var streamers: Map[String, ActorRef] = Map()

  def twGetRequestor(url: String): HttpResponse = {
    val request = new HttpGet(url)
    consumer.sign(request)
    client.execute(request)
  }

  def twPostRequestor(url: String, entity: UrlEncodedFormEntity): HttpResponse = {
    val request = new HttpPost(url)
    request.setEntity(entity);
    consumer.sign(request)
    client.execute(request)
  }

  def rawResponse(response: HttpResponse) = {
    Logger.debug(response.getStatusLine().getStatusCode().toString)
    Ok(IOUtils.toString(response.getEntity().getContent()))
  }

  def followers = Action {
    request =>
      val response = twGetRequestor("https://api.twitter.com/1.1/followers/ids.json?cursor=-1&screen_name=bharathtalks")
      rawResponse(response)
  }

  def mentions = Action {
    request =>
      val response = twGetRequestor("https://api.twitter.com/1.1/statuses/mentions_timeline.json?count=1")
      rawResponse(response)
  }

  def sample = Action {
    request =>

      val nvps = List(new BasicNameValuePair("", ""))
      val snvps = JavaConversions.seqAsJavaList(nvps)
      val uefe = new UrlEncodedFormEntity(snvps)

      val response = twPostRequestor("https://stream.twitter.com/1.1/statuses/sample.json", uefe)
      rawResponse(response)
  }

  /*def go(query: String) = Action {
    val system = ActorSystem()
    val processor = system.actorOf(Props(new TweetProcessor(query)))
    val stream = system.actorOf(Props(new TweetStreamerActor(TweetStreamerActor.twitterUri, processor) with OAuthTwitterAuthorization))
    stream ! query

    Cache.getByteStream(query) match {
      case Some(byteStream) => {
        Ok.chunked(Enumerator.fromStream(new ByteArrayInputStream(byteStream.toByteArray)).andThen(Enumerator.eof)).as("text/html")
      }
      case None => {
        Ok.chunked(Enumerator("something went wrong").andThen(Enumerator.eof)).as("text/html")
      }
    }
  }*/

  def startStreamerProcessor(query: String): ActorRef = {
    val processor: ActorRef = processors.find(_._1 == query).map(_._2) match {
      case Some(actor) => {
        Logger.info(s"using existing processor. actor = $query")
        actor
      }
      case None => {
        Logger.info(s"creating NEW processor. actor = $query")
        val system = ActorSystem()
        val newprocessor = system.actorOf(Props(new TweetProcessor(query)), name = s"processor-$query")
        processors += (query -> newprocessor)
        newprocessor
      }
    }

    val streamer: ActorRef = streamers.find(_._1 == query).map(_._2) match {
      case Some(actor) => {
        Logger.info(s"using existing streamer. actor = $query")
        actor
      }
      case None => {
        Logger.info(s"creating NEW streamer. actor = $query")
        val system = ActorSystem()
        val streamer = system.actorOf(Props(new TweetStreamerActor(TweetStreamerActor.twitterUri, processor) with OAuthTwitterAuthorization),
          name = s"streamer-$query")
        streamers += (query -> streamer)
        streamer
      }
    }
    streamer
  }

  def dashboard(query: String) = Action {
    implicit request =>
      Query.addToQuery(query)
      val streamer = startStreamerProcessor(query)
      streamer ! query
      val wsURL: String = controllers.twitter.routes.Application.live(0).webSocketURL()
      Ok(views.html.dashboard(Query.getStubs)(wsURL))
  }

  def elections(period: Int) = Action {
    implicit request =>

      Query.addToQuery("india")
      Query.addToQuery("modi")
      Query.addToQuery("rahul")
      Query.addToQuery("kejri")
      val query: String = Query.getQuery

      val streamer = startStreamerProcessor(query)
      streamer ! query

      val wsUrl = controllers.twitter.routes.Application.live(period).webSocketURL()
      Ok(views.html.dashboard(Query.getStubs)(wsUrl))
  }

  def live(period: Int) = WebSocket.async[JsValue] {
    request =>
      Statistics.attach(period)
  }

}