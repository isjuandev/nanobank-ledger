# NanoBank Ledger

Aplicación full-stack para gestión de billeteras y transacciones personales, con backend en Spring Boot + JWT y frontend en Angular.

## Decisión de Base de Datos

Se eligió **PostgreSQL** para este proyecto por consistencia transaccional y soporte SQL robusto en un dominio financiero.

Comparativo para este caso:

- **PostgreSQL (elegida)**
  - ACID completo y confiable para operaciones críticas como transferencias entre billeteras.
  - Integración natural con Spring Data JPA y Flyway.
  - Permite constraints e índices útiles para reglas de negocio (`CHECK`, `FK`, índices por `owner_id`, `wallet_id`, `date`).
- **H2 (descartada para persistencia principal)**
  - Buena para pruebas/local rápido, pero no ideal como base principal del examen por diferencias de comportamiento SQL respecto a producción.
  - Riesgo de "funciona en H2, falla en PostgreSQL" en tipos, constraints o dialecto.
- **MongoDB (descartada para este dominio)**
  - Útil para esquemas flexibles, pero aquí hay modelo relacional claro (`users`, `wallets`, `transactions`) y fuerte dependencia de integridad referencial.
  - Transferencias y reglas de saldo se benefician más de transacciones y constraints relacionales.

Conclusión: PostgreSQL es la opción más alineada con integridad financiera, trazabilidad y evolución segura del esquema.

## Cómo ejecutar el proyecto

### Prerrequisitos

- Java 21
- Maven 3.9+
- Node.js 20+
- npm 10+
- Docker (opcional, para PostgreSQL)

### 1) Levantar PostgreSQL (opción Docker)

```bash
docker run --name nanobank-postgres \
  -e POSTGRES_DB=nanobank_ledger \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -d postgres:16
```

### 2) Ejecutar backend

Desde la raíz del proyecto:

```bash
mvn clean spring-boot:run
```

Backend disponible en `http://localhost:8080`.
Flyway aplicará migraciones desde `src/main/resources/db/migration`.

### 3) Ejecutar frontend

```bash
cd frontend
npm install
npm start
```

Frontend disponible en `http://localhost:4200`.

### 4) Ejecutar pruebas

Backend:

```bash
mvn clean test jacoco:report
```

Frontend:

```bash
cd frontend
npm run test:coverage
```

## Arquitectura

Se aplica arquitectura en capas por dominio, separando responsabilidades por paquetes:

- `auth`: autenticación/registro, repositorio de usuarios y DTOs de auth.
- `security`: JWT, filtro de autenticación y configuración de seguridad.
- `wallet`: entidad, controlador, servicio, mapper y repositorio de billeteras.
- `transaction`: entidad, controlador, servicio, repositorio y lógica de transferencia.
- `exception`: excepciones de dominio y `GlobalExceptionHandler`.

Patrón principal:

- **Layered Architecture + Service-Oriented Domain Logic**
  - `controller` expone API.
  - `service` contiene reglas de negocio.
  - `repository` encapsula persistencia.
  - `dto/mapper` desacopla contratos HTTP del modelo persistente.

## Decisiones técnicas de SOLID aplicadas

1. **SRP (Single Responsibility Principle)**
   - Ejemplo: `transaction/service/domain/TransactionBalanceService` concentra reglas de cálculo/ajuste de saldos, evitando mezclarlo con transporte HTTP o acceso directo a repositorio.

2. **OCP (Open/Closed Principle)**
   - Ejemplo: `exception/GlobalExceptionHandler` centraliza el manejo de errores y permite agregar nuevas excepciones de negocio (como `WalletDeletionNotAllowedException`) sin modificar controladores existentes.

3. **DIP (Dependency Inversion Principle)**
   - Ejemplo: controladores y servicios dependen de abstracciones/inyección de dependencias (interfaces y beans de Spring), no de implementaciones concretas creadas manualmente; esto facilita pruebas con mocks y reemplazo de implementaciones.

## Bitácora de Prompts IA

| Prompt objetivo | Resultado de la IA | Criterio Senior aplicado |
|---|---|---|
| Generar filtro JWT para Spring Security | Propuso filtro funcional pero con estilo de inyección inconsistente | Estandaricé inyección por constructor para mejorar testabilidad y cumplir DIP |
| Configurar seguridad de endpoints en Spring Boot 3 | Sugirió enfoque legado (`WebSecurityConfigurerAdapter`) | Reescribí a `SecurityFilterChain` bean por compatibilidad actual y mantenimiento |
| Implementar transferencia entre billeteras | Generó flujo de transferencia sin foco explícito en transaccionalidad | Aseguré operación transaccional para preservar ACID en débito/crédito |
| Crear servicios Angular para auth/wallet/transactions | Entregó servicios válidos con patrones mixtos | Unifiqué estilo con `inject()` en Angular moderno para consistencia del proyecto |
| Generar tests de montos con BigDecimal | Incluyó comparaciones frágiles con `assertEquals` directo | Ajusté validación usando `compareTo()` para evitar falsos negativos por escala |

## Cobertura de Tests

### Backend (JaCoCo)

Generar reporte:

```bash
mvn clean test jacoco:report
```

Resultado HTML:

- `target/site/jacoco/index.html`

### Frontend (Jest/Karma-style coverage report)

Generar reporte:

```bash
cd frontend
npm run test:coverage
```

Resultado HTML:

- `frontend/coverage/lcov-report/index.html`

### Evidencia para entrega del examen

Adjuntar screenshot de ambos reportes:

- JaCoCo backend (`target/site/jacoco/index.html`)
- Coverage frontend (`frontend/coverage/lcov-report/index.html`)
