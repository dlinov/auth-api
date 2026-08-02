package io.dlinov.auth.routes.docs

import cats.effect.Async
import io.dlinov.auth.BuildInfo
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

/** Serves generated OpenAPI 3 documentation and a Swagger UI for `ApiEndpoints.all`, mounted
  * anonymously (same as `/health`) since API documentation is not itself sensitive and every
  * documented operation still enforces its own authentication/authorization as before. Defaults
  * (unchanged): UI at `/docs`, raw spec at `/docs/docs.yaml`.
  */
class DocsRoutes[F[_]: Async] {
  private val swaggerEndpoints =
    SwaggerInterpreter().fromEndpoints[F](ApiEndpoints.all, "auth-api", BuildInfo.version)

  val routes: HttpRoutes[F] = Http4sServerInterpreter[F]().toRoutes(swaggerEndpoints)
}
