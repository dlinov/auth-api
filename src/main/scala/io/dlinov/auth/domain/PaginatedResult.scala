package io.dlinov.auth.domain

final case class PaginatedResult[T](total: Int, results: Seq[T], limit: Int, offset: Int)
