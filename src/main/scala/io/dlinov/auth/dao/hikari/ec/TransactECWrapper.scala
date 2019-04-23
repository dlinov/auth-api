package io.dlinov.auth.dao.hikari.ec

import scala.concurrent.ExecutionContext

class TransactECWrapper(val ec: ExecutionContext) extends AnyVal
