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
| Prompt JWT Backend (Spring Security 6) <br><br> "Eres un Backend Engineer Senior en Spring Boot 3.3 + Java 21. Genera/ajusta un `JwtAuthFilter` para NanoBank Ledger que: 1) lea `Authorization: Bearer <token>`, 2) valide token con `JwtService`, 3) cargue usuario con `UserDetailsServiceImpl`, 4) establezca `SecurityContext` solo si no existe autenticación previa, 5) continúe el `filterChain` sin bloquear requests públicas. Mantén compatibilidad con `SecurityConfig` actual y evita lógica duplicada." | La IA entregó un filtro JWT funcional integrado al pipeline de Spring Security y alineado con extracción/validación del token y carga de usuario. | Estandaricé inyección por constructor, revisé orden del filtro y validé que no sobrescriba autenticación existente para mejorar testabilidad y cumplir DIP/SRP. |
| Prompt Security Endpoints (Spring Boot 3) <br><br> "Configura la seguridad de endpoints en NanoBank Ledger con Spring Security 6, sin usar APIs legacy. Requisitos: `SecurityFilterChain` bean, `SessionCreationPolicy.STATELESS`, `csrf.disable()`, CORS para `http://localhost:4200`, permitir `/api/auth/login` y `/api/auth/register`, proteger el resto, registrar `JwtAuthFilter` antes de `UsernamePasswordAuthenticationFilter`, y definir `AuthenticationProvider` con `UserDetailsServiceImpl` + `PasswordEncoder`." | La IA propuso configuración moderna y compatible con Boot 3, incluyendo cadena stateless, endpoints públicos y registro correcto del filtro JWT. | Rechacé sugerencias legacy (`WebSecurityConfigurerAdapter`) y consolidé configuración por beans para compatibilidad, claridad operativa y mantenimiento a largo plazo. |
| Prompt Transferencias (TransactionServiceImpl) <br><br> "Implementa en `TransactionServiceImpl` la transferencia de una transacción entre billeteras existentes del mismo usuario autenticado. Debe: 1) validar ownership de source/target wallet, 2) soportar no-op cuando source==target, 3) revertir efecto de balance en origen y aplicar efecto en destino según `TransactionType`, 4) persistir wallets y transacción en la misma operación `@Transactional`, 5) lanzar excepciones de dominio existentes (`WalletNotFoundException`, `TransactionNotFoundException`) sin crear nuevas innecesarias." | Se obtuvo un flujo de transferencia funcional usando `TransactionTransferResolver` y `TransactionBalanceService`, con persistencia de wallets y transacción actualizada. | Aseguré transaccionalidad real, cobertura de edge-cases (mismo origen/destino y tipo nulo), y alineación estricta con contrato de excepciones y reglas de saldo del dominio. |
| Prompt 1 (Frontend Core Angular 17) <br><br> "Eres un Angular 17 expert. Genera la **capa core mínima y funcional** para NanoBank Ledger, alineada con el estado actual del proyecto. Usa obligatoriamente Angular 17 API moderna, `inject()`, Signals, `HttpClient`, sin `NgModule`, y genera modelos, servicios (`AuthService`, `WalletService`, `TransactionService`), `auth.interceptor`, `auth.guard`, y `app.config.ts` sin romper rutas existentes ni `register.component.ts`." | Se generó la capa core completa (modelos, servicios, guard, interceptor y configuración de `provideHttpClient`/`provideRouter`) siguiendo standalone + `inject()`. | Validé compatibilidad con estructura actual y limité alcance a core/config para evitar deuda por sobre-ingeniería. |
| Prompt 2 (Frontend Features Angular 17 + CDK) <br><br> "Eres un Angular 17 expert en Signals y Angular CDK. Genera los componentes principales de NanoBank Ledger **sin romper la estructura actual**. Todo standalone + `inject()`: `dashboard.component.ts`, `wallet-card.component.ts`, `transaction-list.component.ts`, `transaction-item.component.ts`, `login.component.ts`; con Signals, computed, drag&drop CDK, actualización optimista y rollback." | Se generaron componentes principales standalone con señales, integración de servicios y flujo drag&drop entre billeteras con manejo optimista y reversión en error. | Priorizé mantener rutas `/login`, `/register`, `/dashboard` y compatibilidad de compilación con el estado real del frontend. |
| Prompt 3 (Testing Angular 17 con Jest/TestBed) <br><br> "Genera los tests de Jest/Jasmine para Angular 17 en NanoBank Ledger, orientados a cobertura >= 80% sobre la capa existente. Usar `provideHttpClientTesting()`, `TestBed.runInInjectionContext()` para signals cuando aplique, mocks explícitos de `localStorage`, y crear specs para `transaction-list.component`, `auth.service`, `wallet.service`, `auth.interceptor`." | Se definieron pruebas unitarias enfocadas en comportamiento crítico: filtros por signals, transferencias con rollback, almacenamiento de token, estado de autenticación y headers de interceptor. | Enfoqué los tests en riesgos funcionales y contratos HTTP reales para cobertura útil (no solo cobertura superficial). |
| Prompt QA Backend (JUnit 5 + Mockito) <br><br> "Eres un QA Engineer Senior con JUnit 5 y Mockito. Genera tests unitarios para NanoBank Ledger sobre clases reales del backend: `WalletServiceImplTest`, `TransactionServiceImplTest`, `AuthServiceTest`; cubrir casos de creación, ownership, transferencia, filtros y autenticación; usar `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks` y en montos `BigDecimal.compareTo()`." | Se implementaron y ajustaron pruebas en `src/test/java/com/nanobank/ledger/wallet/WalletServiceImplTest.java`, `src/test/java/com/nanobank/ledger/transaction/TransactionServiceImplTest.java` y `src/test/java/com/nanobank/ledger/auth/AuthServiceTest.java`, incluyendo escenarios de balance, transferencias, filtros y errores esperados. | Alineé los asserts y excepciones con el comportamiento real de los servicios (`WalletNotFoundException` para recursos ajenos/no encontrados, y `compareTo()` para BigDecimal) para mantener pruebas estables y fieles al dominio. |
| Prompt Arquitectura Backend (Spring Boot 3.3 + Java 21) <br><br> "Eres un arquitecto Java Senior. Define la estructura completa por dominios para NanoBank Ledger con arquitectura en capas (`controller -> service interface+impl -> repository -> entity`), DTOs separados, paquetes `auth`, `wallet`, `transaction`, `exception`, `security`, uso de Lombok y salida en formato árbol + responsabilidades por clase." | Se consolidó la estructura real del backend por dominios, alineando convenciones existentes (servicios `impl`, mappers, DTOs y subdominio `transaction/service/domain`) con una guía de organización explícita para evolución del proyecto. | Priorizé consistencia con la base existente (nombres y paquetes actuales, incluyendo enums de dominio en español) para evitar refactors innecesarios y mantener trazabilidad técnica. |
| Prompt Backend Transferencias (TransactionServiceImpl) <br><br> "Con base en `TransactionServiceImpl`, implementa/ajusta `transferTransaction` para: validar usuario por correo, resolver ownership de source/target wallet, evitar no-op cuando source==target, revertir efecto en origen y aplicar efecto en destino según `TransactionType`, persistir wallets + transacción dentro de `@Transactional`, y lanzar excepciones de dominio existentes." | La IA generó una versión funcional de transferencia con persistencia y validaciones base, usando `TransactionTransferResolver` + `TransactionBalanceService`. | Revisé edge-cases (mismo origen/destino, `type` nulo, ownership) y dejé la lógica alineada al contrato actual de excepciones del dominio. |
| Prompt Backend Wallets (WalletServiceImpl) <br><br> "Ajusta `WalletServiceImpl` para que `deleteWallet` valide ownership antes de eliminar, rechace borrado con transacciones (`WalletDeletionNotAllowedException`) y retorne `WalletNotFoundException` cuando no aplique acceso. Mantén compatibilidad con tests existentes." | Se propuso flujo de borrado con validaciones de negocio y conteo de transacciones por billetera. | Homologué mensajes/excepciones al comportamiento esperado del proyecto para evitar tests frágiles por diferencias semánticas. |
| Prompt Backend Security (SecurityConfig + JwtAuthFilter) <br><br> "Refactoriza seguridad para Spring Boot 3: `SecurityFilterChain`, `SessionCreationPolicy.STATELESS`, endpoints públicos `/api/auth/login` y `/api/auth/register`, JWT filter antes de `UsernamePasswordAuthenticationFilter`, CORS para `http://localhost:4200` y sin `WebSecurityConfigurerAdapter`." | Se obtuvo configuración moderna y compatible con Spring Security 6, incluyendo CORS y pipeline JWT. | Verifiqué que el filtro JWT no rompa pruebas de controladores y que el contrato de endpoints públicos/privados quede explícito. |

## Uso responsable de IA en este proyecto

- Se usa IA para acelerar borradores técnicos, no para reemplazar criterio de arquitectura ni reglas de negocio.
- Todo output generado por IA se valida contra clases reales del proyecto (`service`, `controller`, `repository`, `test`).
- No se aceptan cambios que introduzcan APIs legacy, dependencias innecesarias o refactors masivos fuera del alcance.
- Cada prompt exige compatibilidad con estructura actual y con comportamiento esperado por pruebas.
- Decisión final siempre manual: la IA propone, el desarrollador filtra, corrige y decide.

## Prompts sugeridos para partes del backend

### 1) Auth (registro/login)

```text
Eres un Backend Engineer Senior en Spring Boot 3.3 y Java 21.
Trabaja SOLO sobre las clases reales de NanoBank Ledger: AuthController, AuthServiceImpl, UserRepository, JwtService.
Objetivo: reforzar flujo register/login con validaciones mínimas, respuesta AuthResponse consistente y manejo de errores por GlobalExceptionHandler.
Restricciones:
- No crear nuevas capas ni cambiar contratos HTTP existentes.
- Mantener JWT stateless y compatibilidad con SecurityConfig actual.
- Entregar diff puntual + explicación de riesgos.
```

### 2) Wallet (ownership y borrado seguro)

```text
Actúa como revisor de negocio financiero.
Ajusta WalletServiceImpl para garantizar:
1) ownerEmail siempre valida usuario existente.
2) deleteWallet solo elimina si la billetera pertenece al usuario autenticado.
3) si hay transacciones asociadas, lanzar WalletDeletionNotAllowedException.
4) conservar WalletNotFoundException para accesos inválidos.
No cambies nombres de métodos públicos ni DTOs.
```

### 3) Transaction (transferencias y filtros)

```text
Implementa mejoras en TransactionServiceImpl usando componentes existentes:
- TransactionBalanceService
- TransactionTransferResolver
- TransactionRepository y WalletRepository
Requisitos:
- createTransaction y transferTransaction deben ser @Transactional.
- Manejar no-op transfer cuando sourceWallet == targetWallet.
- Usar compareTo para montos cuando aplique validación de saldo.
- getTransactions debe respetar filtros por categoría y rango de fechas sin romper contrato actual.
```

### 4) Exception Handling (contrato de errores)

```text
Refuerza GlobalExceptionHandler para devolver ErrorResponse homogéneo.
Casos: WalletNotFoundException, TransactionNotFoundException, WalletDeletionNotAllowedException, UnauthorizedResourceException y excepciones genéricas.
No exponer stacktrace en response body.
Asegura mensajes útiles para frontend manteniendo seguridad de la información.
```

## Prompts para corregir tests fallidos o mal implementados

### Prompt A: Corregir test que asume excepción incorrecta

```text
Eres QA Engineer Senior (JUnit5 + Mockito).
Revisa este test fallido y compáralo con el comportamiento REAL del servicio:
- Si el test espera UnauthorizedResourceException pero el servicio retorna WalletNotFoundException para recursos ajenos/no encontrados, corrige el test, no el servicio.
- Mantén patrón AAA y nombres Given_When_Then.
- Usa assertThrows y verifica message cuando sea estable.
Entrega versión final del test y justificación de por qué la expectativa original era incorrecta.
```

### Prompt B: Corregir comparaciones inestables con BigDecimal

```text
Corrige tests de montos en NanoBank Ledger:
- Reemplaza assertEquals(BigDecimal, BigDecimal) por compareTo() == 0 cuando la escala pueda variar.
- Evita falsos negativos por 10.0 vs 10.00.
- No alteres la lógica de negocio para "hacer pasar" tests.
Entrega cambios en WalletServiceImplTest y TransactionServiceImplTest si aplica.
```

### Prompt C: Corregir test de integración de seguridad roto

```text
Depura tests de controladores con MockMvc en Spring Boot 3:
- Verifica endpoints públicos: /api/auth/login y /api/auth/register.
- Verifica endpoints protegidos con JWT.
- Si falla por configuración de seguridad, ajustar setup del test (@WebMvcTest, @MockBean, filtros) antes de tocar lógica productiva.
Genera corrección mínima y explica causa raíz del fallo.
```

## Cobertura de Tests

| Fecha | Módulo | Estado | Tests | Cobertura principal | Detalle de cobertura |
|---|---|---|---|---|---|
| 2026-04-24 | Backend (JaCoCo) | ✅ OK | 90 ejecutados, 0 fallos, 0 errores, 0 skipped | Líneas: 96.29% | Instrucciones: 92.99%, Ramas: 80.00%, Métodos: 89.11%, Clases: 100% |
| 2026-04-24 | Frontend (Jest) | ✅ OK | 46 passed (11 suites), 0 fallos | Líneas: 90.48% | Statements: 90.66%, Branches: 78.57%, Functions: 95.28% |

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
