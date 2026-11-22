import { createBuilder } from './.aspire/modules/aspire.mjs';

const builder = await createBuilder();

// Java microservices: each built from its own Dockerfile, listening on container port 8080.
// withOtlpExporter injects OTEL_* env vars for the OpenTelemetry Java agent (the AddServiceDefaults equivalent).
// The JVM ignores SSL_CERT_DIR, so hand the agent Aspire's dev-cert bundle to trust the HTTPS OTLP endpoint.
for (const name of ['catalog', 'basket', 'ordering']) {
    await builder
        .addDockerfile(name, `./java-back/${name}`)
        .withHttpEndpoint({ targetPort: 8080 })
        .withHttpHealthCheck({ path: '/health' })
        .withOtlpExporter()
        .withCertificateTrustEnvironment('OTEL_EXPORTER_OTLP_CERTIFICATE');
}

// Data infrastructure: one PostgreSQL server (one database per service) with pgAdmin, and Redis for Basket.
// Not wired to the services yet (PLAN.md 2.2).
const postgres = await builder.addPostgres('postgres').withPgAdmin();
await postgres.addDatabase('catalogdb');
await postgres.addDatabase('orderingdb');

await builder.addRedis('redis');

await builder.build().run();
