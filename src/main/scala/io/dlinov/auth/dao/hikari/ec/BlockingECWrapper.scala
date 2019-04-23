package io.dlinov.auth.dao.hikari.ec

import scala.concurrent.ExecutionContext

class BlockingECWrapper(val ec: ExecutionContext) extends AnyVal
