package io.dlinov.auth.routes.dto

case class CollectionResponse[T](results: Seq[T], total: Long)
