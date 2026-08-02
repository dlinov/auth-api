## API documentation (OpenAPI / Swagger UI)

Every live HTTP route is described by a Tapir `AnyEndpoint` (see
`io.dlinov.auth.routes.docs.ApiEndpoints`) and served as generated OpenAPI 3 documentation with
an interactive Swagger UI, mounted anonymously (documentation itself isn't sensitive; every
documented operation still enforces its own authentication/permission as before):

* Swagger UI: `GET /docs`
* Raw OpenAPI 3 spec (YAML): `GET /docs/docs.yaml`

This is a "gradual bridge": the Tapir endpoint definitions exist purely to generate
docs/schemas, they do not replace the existing, tested http4s route handlers or the
`Authenticated` middleware, which remain the actual request-handling/authorization path.

## Configuration

Configuration is loaded via PureConfig from `application.conf` (see
`src/main/resources/application.conf`) under the `fp` root key, with most values overridable via
environment variables (see the `${?VAR_NAME}` fallbacks in that file). Notable sections:

* `fp.db` - database connection/pool settings (`DATABASE_URL`, `DATABASE_USER`,
  `DATABASE_PASSWORD`, `DATABASE_MIN_IDLE_CONN`, `DATABASE_MAX_POOL_SIZE`).
* `fp.auth` - token expiration, account lockout, captcha, recaptcha settings.
* `fp.email` - outbound SMTP settings used for notifications (password reset, etc).
* `fp.proxy` - the downstream host/port that `/api/accounts`, `/api/transactions`,
  `/api/types`, `/api/currencies` and `/api/account_types` reverse-proxy to
  (`PROXY_HOST`, `PROXY_PORT`).
* `fp.server` - conservative server-hardening defaults, all overridable:
  * `idle-timeout` (`SERVER_IDLE_TIMEOUT`, default `60 seconds`): how long a keep-alive
    connection may sit idle before the server closes it.
  * `header-timeout` (`SERVER_HEADER_TIMEOUT`, default `30 seconds`): how long the server waits
    to receive a request's headers before aborting the connection.
  * `request-timeout` (`SERVER_REQUEST_TIMEOUT`, default `45 seconds`): overall wall-clock
    budget for producing a response to a single request.
  * `max-request-body-bytes` (`SERVER_MAX_REQUEST_BODY_BYTES`, default `10485760`, i.e. 10 MiB):
    maximum accepted request body size, enforced before it is buffered/parsed.

## Reverse proxy: security boundary

`AccountsProxy`, `TransactionsProxy` and `TypesProxy` forward authenticated requests to a
downstream API (`fp.proxy`). This service enforces, for every proxied request:

* **Authentication**: a valid bearer token identifying a known back office user (via the same
  `Authenticated` middleware used by every other authenticated route - there is no anonymous/
  public wildcard proxy endpoint).
* **Coarse-grained, scope-level authorization**: the caller must hold the relevant permission
  scope (e.g. `accounts_detail`/`accounts` for `GET /api/accounts/...`, `types`/`types_detail`
  for the read-only `/api/types`, `/api/currencies`, `/api/account_types` roots) before a
  request is forwarded at all.
* **Header hygiene**: inbound `Authorization`, `Host`, `Connection` and standard hop-by-hop
  headers (plus any extra headers named by an inbound `Connection` header) are stripped before
  forwarding; `X-UserName` is always overwritten with the authenticated caller's user name
  (never trusted from the client); the destination host/port is always the configured
  `fp.proxy` value, never derived from client-supplied data. The same hop-by-hop header set is
  stripped from the downstream response before it is returned to the client.

**What it explicitly does *not* enforce: record-level (row/resource) ownership.** For example,
a user permitted to read `accounts_detail` can request *any* account id under `/api/accounts` -
this service has no notion of "does this particular back office user actually own or administer
*this* particular account/transaction", because that requires a business ownership contract
that does not exist in this codebase (and inventing one here would be guessing at a business
rule silently). If per-record ownership authorization is required, **the downstream API itself
must enforce it**; this proxy only guarantees that the caller is an authenticated user holding
the coarse scope-level permission for the operation, and forwards the request (with headers
sanitized as above) unchanged otherwise.
