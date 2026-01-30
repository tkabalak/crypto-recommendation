# crypto-recommendation-app

Spring Boot application module. See root `README.md` for full documentation.


## Swagger UI
- http://localhost:8080/swagger-ui

## Tests + coverage (JaCoCo >= 80%)
```bash
mvn clean verify
```

## CSV import
Place `*_values.csv` files in:
- `src/main/resources/data/`

Import runs at startup by default (see `application.yml`).
