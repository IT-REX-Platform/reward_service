# Reward Service

The service manages five types of reward scores: Health, Fitness, Growth, Strength, and Power.

1. **Health Score:** Reflects a user's learning progress, increasing when they learn new content and decreasing when they fall behind.

2. **Fitness Score:** Measures how well a student revisits previous chapters, encouraging review of old content and considering correctness scores.

3. **Growth Score:** Serves as a progress indicator, showing students how much of the course lies ahead as they learn new concepts.

4. **Strength Score:** Encourages participation in REX-Duels, promoting interaction among students and rewarding winners with more points.

5. **Power Score:** Represents a composite value of other reward properties, enabling student ranking based on overall performance.

For more details about the Reward Service Scoring System, please refer to the [documentation](https://gits-enpro.readthedocs.io/en/latest/dev-manuals/gamification/Scoring%20System.html).

## Environment variables

### Relevant for deployment

| Name                       | Description                        | Value in Dev Environment                        | Value in Prod Environment                                          |
|----------------------------|------------------------------------|-------------------------------------------------|--------------------------------------------------------------------|
| spring.datasource.url      | PostgreSQL database URL            | jdbc:postgresql://localhost:7032/reward_service | jdbc:postgresql://reward-service-db-postgresql:5432/reward-service |
| spring.datasource.username | Database username                  | root                                            | gits                                                               |
| spring.datasource.password | Database password                  | root                                            | *secret*                                                           |
| DAPR_HTTP_PORT             | Dapr HTTP Port                     | 7000                                            | 3500                                                               |
| server.port                | Port on which the application runs | 7001                                            | 7001                                                               |

### Other properties
| Name                                    | Description                               | Value in Dev Environment                | Value in Prod Environment                                        |
|-----------------------------------------|-------------------------------------------|-----------------------------------------|------------------------------------------------------------------|
| spring.graphql.graphiql.enabled         | Enable GraphiQL web interface for GraphQL | true                                    | true                                                             |
| spring.graphql.graphiql.path            | Path for GraphiQL when enabled            | /graphiql                               | /graphiql                                                        |
| spring.profiles.active                  | Active Spring profile                     | dev                                     | prod                                                             |
| spring.jpa.properties.hibernate.dialect | Hibernate dialect for PostgreSQL          | org.hibernate.dialect.PostgreSQLDialect | org.hibernate.dialect.PostgreSQLDialect                          |
| spring.sql.init.mode                    | SQL initialization mode                   | always                                  | always                                                           |
| spring.jpa.show-sql                     | Show SQL queries in logs                  | true                                    | true                                                             |
| spring.sql.init.continue-on-error       | Continue on SQL init error                | true                                    | true                                                             |
| spring.jpa.hibernate.ddl-auto           | Hibernate DDL auto strategy               | create                                  | update                                                           |
| DAPR_GRPC_PORT                          | Dapr gRPC Port                            | -                                       | 50001                                                            |
| reward.recalculation.cron               | Cron expression for recalculation         | 0 0 3 * * *                             | 0 0 3 * * *                                                      |
| content_service.url                     | URL for content service GraphQL           | http://localhost:4001/graphql           | http://localhost:3500/v1.0/invoke/content-service/method/graphql |

## API description

The GraphQL API is described in the [api.md file](api.md).

The endpoint for the GraphQL API is `/graphql`. The GraphQL Playground is available at `/graphiql`.

## How to run

How to run services locally is described in
the [wiki](https://gits-enpro.readthedocs.io/en/latest/dev-manuals/backend/get-started.html).

