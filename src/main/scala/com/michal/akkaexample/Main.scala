package com.michal.akkaexample

import java.io.File
import java.nio.file.Paths

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ActorMaterializer, IOResult, Materializer}
import akka.http.scaladsl.unmarshalling._
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.common.JsonEntityStreamingSupport
import akka.stream.scaladsl.{FileIO, Sink, Source}
import akka.util.ByteString
import spray.json._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

//Saves all posts from https://jsonplaceholder.typicode.com/posts
// to a directory passed as command-line parameter
object Main extends App with PostJsonSupport {

  val uri = "https://jsonplaceholder.typicode.com/posts"
  val parallelism = 4

  implicit val jsonStreamingSupport: JsonEntityStreamingSupport =
    EntityStreamingSupport.json()


  args.headOption match {
    case Some(targetDirectoryPath) if isDirectory(targetDirectoryPath) =>
      readPosts(targetDirectoryPath)
      println("posts saved successfully")
    case Some(targetDirectoryPath)  =>
      println("No posts read - invalid target directory path passed")
    case None =>
      println("No posts read - 1st argument should contain path to the target directory")
  }

  def isDirectory(path: String): Boolean = {
    import java.nio.file.{Paths, Files}
    Files.isDirectory(Paths.get(path))
  }

  def readPosts(targetDirectory: String) = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    implicit val executionContext = system.dispatcher

    def shutDownActorSystem() = {
      Await.result(Http().shutdownAllConnectionPools(), Duration.Inf)
      materializer.shutdown()
      Await.result(system.terminate(), Duration.Inf)
    }

    val responseFuture = Http().singleRequest(HttpRequest(uri = uri))

    val futureSource = responseFuture
      .flatMap { response =>
        Unmarshal(response).to[Source[Post, NotUsed]]
      }

    Source
      .fromFutureSource(futureSource)
      .mapAsync(parallelism) { post =>
        saveSinglePost(post, targetDirectory)
      }.runWith(Sink.ignore)
      .onComplete { _ =>shutDownActorSystem()}
  }

  def saveSinglePost(post: Post, targetDirectory: String)(implicit m: Materializer): Future[IOResult] = {
    val fileName = targetDirectory + File.separator + post.id +  ".json"
    Source.single(ByteString(post.toJson.prettyPrint)).runWith(FileIO.toPath(Paths.get(fileName)))
  }

}
