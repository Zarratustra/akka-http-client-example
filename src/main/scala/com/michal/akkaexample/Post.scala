package com.michal.akkaexample

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

case class Post(id: Long, userId: Long, title: String, body: String) {
  def withComments(comments: List[Comment]) = PostWithComments(
    id,
    userId,
    title,
    body,
    comments
  )
}

case class PostWithComments(
                             id: Long,
                             userId: Long,
                             title: String,
                             body: String,
                             comments: List[Comment]
                           )

case class Comment(
                    id: Long,
                    postId: Long,
                    name: String,
                    email: String,
                    body: String
                  )

trait PostJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val postFormat = jsonFormat4(Post)
  implicit val commentFormat = jsonFormat5(Comment)
  implicit val postWithCommentsFormat = jsonFormat5(PostWithComments)
}