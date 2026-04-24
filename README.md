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
| Prompt 1 (Frontend Core Angular 17) <br><br> "Eres un Angular 17 expert. Genera la **capa core mínima y funcional** para NanoBank Ledger, alineada con el estado actual del proyecto. Usa obligatoriamente Angular 17 API moderna, `inject()`, Signals, `HttpClient`, sin `NgModule`, y genera modelos, servicios (`AuthService`, `WalletService`, `TransactionService`), `auth.interceptor`, `auth.guard`, y `app.config.ts` sin romper rutas existentes ni `register.component.ts`." | Se generó la capa core completa (modelos, servicios, guard, interceptor y configuración de `provideHttpClient`/`provideRouter`) siguiendo standalone + `inject()`. | Validé compatibilidad con estructura actual y limité alcance a core/config para evitar deuda por sobre-ingeniería. |
| Prompt 2 (Frontend Features Angular 17 + CDK) <br><br> "Eres un Angular 17 expert en Signals y Angular CDK. Genera los componentes principales de NanoBank Ledger **sin romper la estructura actual**. Todo standalone + `inject()`: `dashboard.component.ts`, `wallet-card.component.ts`, `transaction-list.component.ts`, `transaction-item.component.ts`, `login.component.ts`; con Signals, computed, drag&drop CDK, actualización optimista y rollback." | Se generaron componentes principales standalone con señales, integración de servicios y flujo drag&drop entre billeteras con manejo optimista y reversión en error. | Priorizé mantener rutas `/login`, `/register`, `/dashboard` y compatibilidad de compilación con el estado real del frontend. |
| Prompt 3 (Testing Angular 17 con Jest/TestBed) <br><br> "Genera los tests de Jest/Jasmine para Angular 17 en NanoBank Ledger, orientados a cobertura >= 80% sobre la capa existente. Usar `provideHttpClientTesting()`, `TestBed.runInInjectionContext()` para signals cuando aplique, mocks explícitos de `localStorage`, y crear specs para `transaction-list.component`, `auth.service`, `wallet.service`, `auth.interceptor`." | Se definieron pruebas unitarias enfocadas en comportamiento crítico: filtros por signals, transferencias con rollback, almacenamiento de token, estado de autenticación y headers de interceptor. | Enfoqué los tests en riesgos funcionales y contratos HTTP reales para cobertura útil (no solo cobertura superficial). |
| Prompt QA Backend (JUnit 5 + Mockito) <br><br> "Eres un QA Engineer Senior con JUnit 5 y Mockito. Genera tests unitarios para NanoBank Ledger sobre clases reales del backend: `WalletServiceImplTest`, `TransactionServiceImplTest`, `AuthServiceTest`; cubrir casos de creación, ownership, transferencia, filtros y autenticación; usar `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks` y en montos `BigDecimal.compareTo()`." | Se implementaron y ajustaron pruebas en `src/test/java/com/nanobank/ledger/wallet/WalletServiceImplTest.java`, `src/test/java/com/nanobank/ledger/transaction/TransactionServiceImplTest.java` y `src/test/java/com/nanobank/ledger/auth/AuthServiceTest.java`, incluyendo escenarios de balance, transferencias, filtros y errores esperados. | Alineé los asserts y excepciones con el comportamiento real de los servicios (`WalletNotFoundException` para recursos ajenos/no encontrados, y `compareTo()` para BigDecimal) para mantener pruebas estables y fieles al dominio. |
| Prompt Arquitectura Backend (Spring Boot 3.3 + Java 21) <br><br> "Eres un arquitecto Java Senior. Define la estructura completa por dominios para NanoBank Ledger con arquitectura en capas (`controller -> service interface+impl -> repository -> entity`), DTOs separados, paquetes `auth`, `wallet`, `transaction`, `exception`, `security`, uso de Lombok y salida en formato árbol + responsabilidades por clase." | Se consolidó la estructura real del backend por dominios, alineando convenciones existentes (servicios `impl`, mappers, DTOs y subdominio `transaction/service/domain`) con una guía de organización explícita para evolución del proyecto. | Priorizé consistencia con la base existente (nombres y paquetes actuales, incluyendo enums de dominio en español) para evitar refactors innecesarios y mantener trazabilidad técnica. |

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
