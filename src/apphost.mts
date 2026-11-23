import { createBuilder } from './.aspire/modules/aspire.mjs';

const builder = await createBuilder();

// Data infrastructure: one PostgreSQL server (one database per service) with pgAdmin, and Redis for Basket.
const postgres = await builder.addPostgres('postgres').withPgAdmin();
const catalogdb = await postgres.addDatabase('catalogdb');
const orderingdb = await postgres.addDatabase('orderingdb');

await builder.addRedis('redis');

// Java microservices: each built from its own Dockerfile, listening on container port 8080.
// withOtlpExporter injects OTEL_* env vars for the OpenTelemetry Java agent (the AddServiceDefaults equivalent).
// The JVM ignores SSL_CERT_DIR, so hand the agent Aspire's dev-cert bundle to trust the HTTPS OTLP endpoint.

// Catalog owns catalogdb. Aspire's connection string is not JDBC, so build the SPRING_DATASOURCE_* contract here.
await builder
    .addDockerfile('catalog', './java-back/catalog')
    .withHttpEndpoint({ targetPort: 8080 })
    .withHttpHealthCheck({ path: '/health' })
    .withOtlpExporter()
    .withCertificateTrustEnvironment('OTEL_EXPORTER_OTLP_CERTIFICATE')
    .withReference(catalogdb)
    .waitFor(catalogdb)
    .withEnvironment('SPRING_DATASOURCE_URL', await catalogdb.jdbcConnectionString())
    .withEnvironment('SPRING_DATASOURCE_USERNAME', await postgres.userNameReference())
    .withEnvironment('SPRING_DATASOURCE_PASSWORD', postgres.passwordParameter.get());

// Ordering owns orderingdb, with the same SPRING_DATASOURCE_* contract as catalog.
await builder
    .addDockerfile('ordering', './java-back/ordering')
    .withHttpEndpoint({ targetPort: 8080 })
    .withHttpHealthCheck({ path: '/health' })
    .withOtlpExporter()
    .withCertificateTrustEnvironment('OTEL_EXPORTER_OTLP_CERTIFICATE')
    .withReference(orderingdb)
    .waitFor(orderingdb)
    .withEnvironment('SPRING_DATASOURCE_URL', await orderingdb.jdbcConnectionString())
    .withEnvironment('SPRING_DATASOURCE_USERNAME', await postgres.userNameReference())
    .withEnvironment('SPRING_DATASOURCE_PASSWORD', postgres.passwordParameter.get());

// Not wired to its data store yet (basket/01).
for (const name of ['basket']) {
    await builder
        .addDockerfile(name, `./java-back/${name}`)
        .withHttpEndpoint({ targetPort: 8080 })
        .withHttpHealthCheck({ path: '/health' })
        .withOtlpExporter()
        .withCertificateTrustEnvironment('OTEL_EXPORTER_OTLP_CERTIFICATE');
}

await builder.build().run();
