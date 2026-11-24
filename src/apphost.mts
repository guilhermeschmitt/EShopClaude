import { createBuilder } from './.aspire/modules/aspire.mjs';

const builder = await createBuilder();

// Data infrastructure: one PostgreSQL server (one database per service) with pgAdmin, and Redis for Basket.
const postgres = await builder.addPostgres('postgres').withPgAdmin();
const catalogdb = await postgres.addDatabase('catalogdb');
const orderingdb = await postgres.addDatabase('orderingdb');

// Redis runs without TLS: Aspire would serve TLS with its dev cert, which the JVM doesn't trust.
const redis = await builder.addRedis('redis').withoutHttpsCertificate();

// Java microservices: each built from its own Dockerfile, listening on container port 8080.
// withOtlpExporter injects OTEL_* env vars for the OpenTelemetry Java agent (the AddServiceDefaults equivalent).
// The JVM ignores SSL_CERT_DIR, so hand the agent Aspire's dev-cert bundle to trust the HTTPS OTLP endpoint.

// Catalog owns catalogdb. Aspire's connection string is not JDBC, so build the SPRING_DATASOURCE_* contract here.
const catalog = await builder
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
const ordering = await builder
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

// Basket owns baskets in Redis, looks products up in Catalog and creates orders in Ordering at checkout. Spring needs host/port/password, not Aspire's connection string.
await builder
    .addDockerfile('basket', './java-back/basket')
    .withHttpEndpoint({ targetPort: 8080 })
    .withHttpHealthCheck({ path: '/health' })
    .withOtlpExporter()
    .withCertificateTrustEnvironment('OTEL_EXPORTER_OTLP_CERTIFICATE')
    .withReference(redis)
    .waitFor(redis)
    // Dockerfile containers don't provide service discovery, so withReference(catalog/ordering) is rejected.
    // Set the service-discovery variables from each peer's endpoint instead.
    .withEnvironment('services__catalog__http__0', catalog.getEndpoint('http'))
    .waitFor(catalog)
    .withEnvironment('services__ordering__http__0', ordering.getEndpoint('http'))
    .waitFor(ordering)
    .withEnvironment('SPRING_DATA_REDIS_HOST', await redis.host())
    .withEnvironment('SPRING_DATA_REDIS_PORT', await redis.port())
    .withEnvironment('SPRING_DATA_REDIS_PASSWORD', redis.passwordParameter.get());

await builder.build().run();
