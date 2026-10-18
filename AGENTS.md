# Agent instructions

Buyer-side catalog browsing API for iSaaS (Spring Boot). Project decisions and history are in the knowledge base: https://github.com/isaas-org/isaas-catalog-kb-agent-based-development

## Rules

- Never commit or push to `main`. Work only on the feature branch the repo owner names (pattern `feature/FD-<n>_<Description>`).
- The database schema is owned by `isaas-database`. Don't add migrations here and keep `ddl-auto: none`.
- Real schema names: `items`, `vendors`, `buyer_companies` (not `buyers`).
- Postgres native ENUMs are used for status and role columns. Map them explicitly (for example `@JdbcTypeCode(SqlTypes.NAMED_ENUM)`), don't rely on defaults.
- Catalog listing reads `items.published_catalog`; don't re-implement the published/active filter in Java.
- Business logic stays in this service. Frontends and the agent app call its API and never touch the database.

## Commands

- Build and test: `./mvnw verify` (needs Postgres at `DATABASE_URL`)
- Run: `./mvnw spring-boot:run` (port 8081)
