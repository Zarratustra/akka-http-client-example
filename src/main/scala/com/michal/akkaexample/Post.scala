package com.michal.akkaexample

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

case class Post(id: Long, userId: Long, title: String, body: String)

trait PostJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val postFormat = jsonFormat4(Post)
}