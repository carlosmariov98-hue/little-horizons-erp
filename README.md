# ERP Colegio Little Horizons

Sistema de gestión educativa por consola para el Colegio Little Horizons. Administra estudiantes, acudientes, profesores, matrículas, notas, asistencia, pagos con factura, clubes, eventos, recursos con préstamos y reportes. Está hecho en **Java 17**, **MySQL 8** y **JDBC puro**, sin frameworks, con arquitectura **MVC**.

- **Autor:** [NOMBRE COMPLETO]
- **Repositorio:** [URL DEL REPOSITORIO]
- **Entrega:** 17 de noviembre de 2026

---

## 1. Tabla de contenido

1. [Tabla de contenido](#1-tabla-de-contenido)
2. [Tecnologías y requisitos](#2-tecnologías-y-requisitos)
3. [Instalación y ejecución](#3-instalación-y-ejecución)
4. [Estructura del proyecto](#4-estructura-del-proyecto)
5. [Arquitectura MVC](#5-arquitectura-mvc)
6. [Módulos y reglas de negocio](#6-módulos-y-reglas-de-negocio)
7. [Principios SOLID](#7-principios-solid)
8. [Patrones de diseño](#8-patrones-de-diseño)
9. [Lambdas y Streams](#9-lambdas-y-streams)
10. [Manejo de errores](#10-manejo-de-errores)
11. [Archivos y logs](#11-archivos-y-logs)
12. [JDBC y base de datos](#12-jdbc-y-base-de-datos)
13. [Interfaz de consola](#13-interfaz-de-consola)
14. [Decisiones de diseño](#14-decisiones-de-diseño)
15. [Problemas conocidos](#15-problemas-conocidos)
16. [Control de versiones y cumplimiento de la rúbrica](#16-control-de-versiones-y-cumplimiento-de-la-rúbrica)

---

## 2. Tecnologías y requisitos

| Elemento | Versión / detalle |
|---|---|
| Lenguaje | Java 17 |
| Base de datos | MySQL **8.0 o superior** |
| Acceso a datos | JDBC con MySQL Connector/J 9.7.0 (archivo `.jar` en la carpeta `lib/`) |
| IDE usado | IntelliJ IDEA (proyecto simple, sin Maven ni Gradle) |
| Frameworks | Ninguno |

Solo se usa la biblioteca estándar de Java: `java.sql`, `java.time` (fechas), `java.math.BigDecimal` (dinero y notas), `java.security.MessageDigest` (CUFE de la factura), `java.nio.file` (archivos) y `java.util.stream` (consultas en memoria).

---

## 3. Instalación y ejecución

1. **Clonar** el repositorio:
   ```
   git clone [URL DEL REPOSITORIO]
   ```
2. **Crear la base de datos.** Ejecute en MySQL el script SQL del proyecto, que crea el esquema `erp_little_horizons` con sus tablas y datos de ejemplo:
   ```
   mysql -u root -p --default-character-set=utf8mb4 < [RUTA DEL SCRIPT, por ejemplo database/database.sql]
   ```
3. **Crear un usuario de MySQL** con permisos sobre la base (opcional, también sirve `root`):
   ```sql
   CREATE USER 'erp'@'localhost' IDENTIFIED BY 'su_clave';
   GRANT ALL ON erp_little_horizons.* TO 'erp'@'localhost';
   ```
4. **Configurar la conexión.** Copie `db.properties.example` como `db.properties` en la raíz del proyecto y escriba sus datos:
   ```
   db.url=jdbc:mysql://localhost:3306/erp_little_horizons
   db.user=erp
   db.password=su_clave
   ```
   `db.properties` está en `.gitignore`: las credenciales nunca se suben al repositorio.
5. **Agregar el driver.** En IntelliJ: *File > Project Structure > Libraries > + > Java* y seleccione el `.jar` de `lib/`.
6. **Ejecutar** la clase `com.littlehorizons.erp.Main` (el directorio de trabajo debe ser la raíz del proyecto, porque ahí se leen `db.properties` y se escriben `error_log.txt`, `invoices/` y `reports/`).

Si MySQL no está encendido o `db.properties` no existe, el programa muestra un mensaje claro en español y termina sin mostrar errores técnicos.

---

## 4. Estructura del proyecto

```
little-horizons-erp/
├── lib/                          driver JDBC (.jar)
├── src/com/littlehorizons/erp/
│   ├── Main.java                 raíz de composición: crea y conecta todos los objetos
│   ├── config/                   AppConfig, ConnectionProvider, DatabaseConnection
│   ├── model/                    entidades (Student, Payment, Loan, ...)
│   │   └── enums/                estados y tipos (StudentStatus, PaymentMethod, ...)
│   ├── dao/                      acceso a datos con JDBC (BaseDao y un DAO por entidad)
│   ├── controller/               reglas de negocio y estadísticas con Streams
│   ├── view/                     menús de consola y lectura de datos
│   ├── exception/                jerarquía de excepciones del sistema
│   └── util/                     Validator, AppLogger, DateUtils, MoneyFormat, CUFE, IVA, escritores de archivos
├── db.properties.example         plantilla de configuración
├── .gitignore
└── README.md
```

Archivos que el programa genera al ejecutarse (no se versionan): `error_log.txt`, `invoices/` y `reports/`.

---

## 5. Arquitectura MVC

```
 Usuario ──> view (menús)  ──>  controller (reglas)  ──>  dao (SQL)  ──>  MySQL
              lee y muestra       valida y decide          JDBC
                  ▲                    │
                  └──── model ◄────────┘   (objetos que viajan entre capas)
```

| Capa | Responsabilidad | Lo que NO hace |
|---|---|---|
| **model** | Guarda datos (entidades y enums). Casi sin lógica. | No accede a la base ni imprime. |
| **view** | Muestra menús, lee lo que escribe el usuario y presenta resultados. | No contiene reglas de negocio ni SQL. |
| **controller** | Valida, aplica las reglas de negocio y coordina los DAOs. | No imprime ni lee consola. |
| **dao** | Ejecuta el SQL con `PreparedStatement` y convierte filas en objetos. | No decide reglas de negocio (salvo las que dependen de una transacción). |

Los objetos se conectan por constructor en `Main.java`, que es el único lugar que conoce todas las clases concretas.

---

## 6. Módulos y reglas de negocio

El menú principal tiene seis módulos más la salida:

| # | Módulo | Qué permite |
|---|---|---|
| 1 | **Estudiantes** | Registrar, editar, buscar, cambiar estado y asignar acudientes. |
| 2 | **Académica** | Matrículas por año, inscripción en materias, notas por período (I a IV y Final) y asistencia. |
| 3 | **Profesores** | Registro, estado y asignación de materias por grado y sección. |
| 4 | **Extracurriculares** | Clubes (afiliaciones con cupo) y eventos (participantes, resultados y podio). |
| 5 | **Financiera** | Conceptos de pago, registro de pagos con factura TXT, historial y morosos. |
| 6 | **Recursos** | Inventario y préstamos a estudiantes y profesores. |
| 7 | **Reportes** | Estadísticas de todos los módulos y resumen general exportable. |

### Reglas principales

**Estudiantes, acudientes y profesores**
- El documento del estudiante debe corresponder a su edad; no se repiten documentos entre personas del mismo tipo.
- Un estudiante debe tener **al menos un acudiente**.
- Un profesor no se repite por documento ni por correo.
- No se cambia a un estado en el que ya está.

**Académica y asistencia**
- Solo estudiantes **activos** se matriculan, y solo **una matrícula por año**.
- Una nota va de 0.0 a 5.0 con un decimal. La nota **Final se calcula automáticamente** con los períodos I a IV y no se escribe a mano.
- El nivel de desempeño (Bajo, Básico, Alto, Superior) se calcula con la escala colombiana (Decreto 1290) y no se guarda.
- Solo un profesor **activo** y asignado a esa materia puede calificar o tomar asistencia.
- La asistencia solo se registra en días de lunes a viernes, y el reporte semanal señala a quienes tienen **más de 3 ausencias**.

**Financiera**
- Un pago genera **una factura**. El IVA (19%) solo aplica a *Uniforme* y *Certificado*, y el valor guardado ya lo incluye.
- El CUFE es un hash SHA-384 de número de factura, NIT, y fecha de emisión (simulado).
- Es **moroso** el estudiante activo sin pago de pensión en los últimos 30 días.
- El pago y su factura se guardan en una sola transacción.

**Clubes y eventos**
- El cupo del club se verifica dentro de una transacción con bloqueo de fila (`SELECT ... FOR UPDATE`), así dos afiliaciones simultáneas no superan el máximo.
- Un estudiante no se repite en un club: si se retira y vuelve, se reactiva la misma afiliación.
- Solo estudiantes activos se afilian; un club con miembros activos no se desactiva.
- Los puestos (1.º, 2.º y 3.er lugar) son únicos por evento y solo se asignan en eventos de competencia ya realizados.

**Recursos y préstamos**
- El estado *Prestado* lo asignan **solo los préstamos**; *Dado de Baja* es definitivo.
- Cada persona puede tener **máximo 3 préstamos abiertos** y cada préstamo dura **máximo 60 días**.
- Quien tenga un préstamo vencido no puede pedir otro recurso.
- El préstamo y el estado del recurso cambian juntos en una transacción. Si el recurso vuelve dañado, pasa a mantenimiento.

---

## 7. Principios SOLID

**S, Responsabilidad única.** Cada clase hace una sola cosa:
`AppConfig` solo lee configuración; `DatabaseConnection` solo entrega conexiones; `CufeGenerator` solo calcula el CUFE; `TaxPolicy` solo decide el IVA; `InvoiceFileWriter` y `ReportFileWriter` solo escriben archivos; `Validator` solo valida texto, fechas y números. Los menús solo hablan con el usuario, los controllers solo aplican reglas y los DAOs solo ejecutan SQL.

**O, Abierto a extensión, cerrado a modificación.** Para agregar un DAO nuevo basta con extender `BaseDao` y usar sus ayudantes (`queryList`, `executeUpdate`, `inTransaction`) sin tocar la clase base. `BorrowerType` guarda los nombres de tabla y columna de cada tipo de prestatario: un tercer tipo (por ejemplo, personal administrativo) se agrega como una constante nueva y `LoanDao` no cambia.

**L, Sustitución de Liskov.** Todas las excepciones del sistema extienden `ErpException`, por eso `BaseMenu.runSafely` atrapa cualquiera y la trata igual. Todos los menús y DAOs se usan a través de `BaseMenu` y `BaseDao` sin que quien los llama sepa cuál es.

**I, Segregación de interfaces.** Las interfaces son mínimas y de un solo método: `ConnectionProvider`, `Binder`, `RowMapper`, `TransactionalAction`, `Parser`, `ChoiceValidator` y `MenuAction`. Nadie depende de métodos que no usa.

**D, Inversión de dependencias.** Los DAOs reciben un `ConnectionProvider` (interfaz), no la clase `DatabaseConnection`. Los controllers reciben sus DAOs por constructor y los menús reciben sus controllers por constructor. Todo se ensambla en `Main`, sin `new` escondidos dentro de las clases de negocio.

---

## 8. Patrones de diseño

| Patrón | Dónde | Para qué |
|---|---|---|
| **Singleton** | `AppLogger`, `DatabaseConnection` | Una sola instancia compartida del log y de la configuración de conexión. `AppLogger` se crea al cargar la clase; `DatabaseConnection.getInstance()` es `synchronized`. |
| **DAO** | Paquete `dao` | Aísla todo el SQL. El resto del sistema nunca ve `Connection` ni `ResultSet`. |
| **Template Method (con lambdas)** | `BaseDao.inTransaction`, `BaseMenu.runSafely` | La clase base fija los pasos fijos (abrir, confirmar o deshacer, cerrar; atrapar errores y volver al menú) y la lambda que recibe aporta lo variable. |
| **Strategy (con lambdas)** | `Binder`, `RowMapper`, `Parser` | Cada consulta entrega su propia forma de llenar parámetros y de leer filas. |
| **Inyección de dependencias** | Constructores y `Main` | Las clases reciben lo que necesitan, lo que facilita cambiarlas o probarlas. |
| **Fachada** | `ReportController` | Una entrada única que consulta y combina varios módulos para los reportes. |

---

## 9. Lambdas y Streams

Los Streams y lambdas hacen el trabajo real de cálculo; no son decorativos.

| Dónde | Qué hace | Operaciones |
|---|---|---|
| `ReportController.studentAverages` | Promedio por estudiante y orden de mejor a peor | `groupingBy`, `collectingAndThen`, `map`, `sorted(Comparator...)` |
| `ReportController.subjectAverages` | Promedio por materia | `groupingBy` con `TreeMap` |
| `ReportController.levelDistribution` | Estudiantes por nivel de desempeño | `groupingBy`, `counting` |
| `ReportController.incomeByMonth` | Recaudo por mes | `groupingBy`, `reducing` |
| `ReportController.topAbsences` | Estudiantes con más ausencias | `filter`, `groupingBy`, `sorted`, `limit` |
| `PaymentController.getDelinquentStudents` | Morosos | `map`, `filter`, `sorted` con `nullsFirst` |
| `PaymentController.totalsByConcept` | Totales por concepto | `groupingBy`, `reducing` |
| `LoanController.getMostBorrowed` | Recursos más prestados | `groupingBy`, `counting`, `sorted`, `limit`, `toMap` |
| `LoanController.averageDaysLate` | Retraso promedio con `BigDecimal` | `filter`, `map`, `reduce` |
| `ResourceController.availableByCategory` | Disponibles por categoría | `filter`/`groupingBy` con `TreeMap` |
| `EventController` | Podio y eventos por tipo | `groupingBy`, `counting`, `sorted` |
| Menús (`BaseMenu`, `InputReader`) | Acciones y validaciones como lambdas | `MenuAction`, `Parser`, referencias a métodos (`this::lend`) |

---

## 10. Manejo de errores

```
Exception
└── ErpException (verificada)
    ├── ValidationException      dato mal escrito (formato, rango, longitud)
    ├── BusinessRuleException    regla de negocio rota (cupo lleno, préstamo vencido...)
    ├── EntityNotFoundException  el registro no existe
    ├── DatabaseException        falla de MySQL o JDBC
    ├── ConfigurationException   falta o falla db.properties
    └── FileOperationException   falla al escribir una factura o un reporte
RuntimeException
└── OperationCancelledException  el usuario escribió "cancelar" (solo capa de vista)
```

- Los mensajes que ve el usuario están en **español** y no revelan detalles técnicos.
- El detalle técnico (consulta, id, stack trace) se escribe en `error_log.txt` en **inglés**, en el lugar donde ocurre el error.
- `OperationCancelledException` es una excepción de flujo de la interfaz, por eso vive en `view` y no extiende `ErpException`: cancelar no es un error.
- Cualquier `RuntimeException` inesperado se registra en el log y el programa termina con un mensaje, sin mostrar la traza.

---

## 11. Archivos y logs

| Archivo | Cuándo se genera | Contenido |
|---|---|---|
| `error_log.txt` | Ante cualquier error técnico | Una línea con fecha, hora, nivel (`INFO`, `WARN`, `ERROR`) y mensaje, más el stack trace si existe. Solo se agrega al final, nunca se borra. |
| `invoices/factura_XXXXXX.txt` | Al registrar un pago | Factura simulada con datos del cliente, detalle, IVA, total, CUFE y texto QR. |
| `reports/resumen_general_AAAAMMDD.txt` | Al guardar el resumen del menú de reportes | Los números principales del colegio. |

Ejemplo de línea del log:
```
2026-11-17 10:42:08 [ERROR] Could not lend resource id 3 to STUDENT id 30.
```

Todos los archivos se escriben con `java.nio.file`, en UTF-8, con manejo de `IOException`. Si el archivo de una factura falla después de guardar el pago, el pago **no se pierde**: el sistema avisa y permite generar la factura de nuevo.

---

## 12. JDBC y base de datos

- **PreparedStatement en todas las consultas.** Ningún dato del usuario se concatena en el SQL, lo que evita inyección SQL. Las únicas partes dinámicas del SQL son nombres de tabla o columna que salen de constantes del enum `BorrowerType`, nunca de lo que escribe el usuario.
- **try-with-resources** en cada `Connection`, `PreparedStatement` y `ResultSet`.
- **Transacciones** con `BaseDao.inTransaction`: se desactiva el autocommit, se confirma al final y se deshace automáticamente si algo falla. Se usan al registrar pagos con factura, afiliar a clubes, prestar y devolver recursos.
- **Bloqueo de filas** con `SELECT ... FOR UPDATE` para evitar condiciones de carrera (cupos de clubes, recursos prestados a dos personas a la vez).
- **Errores de MySQL traducidos**: el código 1062 (dato duplicado) y 1452 (llave foránea) se convierten en mensajes claros para el usuario.
- **Enums con etiqueta de base de datos**: cada enum guarda el texto exacto de la columna (`"Disponible"`, `"En Mantenimiento"`) y un método `fromLabel`.
- **DML** usado: `INSERT`, `UPDATE` y `SELECT` con `JOIN`, `GROUP BY` y `COUNT`/`SUM`. No se eliminan registros: se cambia su estado, para conservar el historial.

---

## 13. Interfaz de consola

- Menús numerados con banners y títulos, y mensajes con prefijo `[ÉXITO]` o `[ERROR]`.
- **Cada dato se vuelve a pedir** hasta que sea válido (`InputReader.readValid`), con un mensaje que explica el formato esperado.
- Se puede escribir **`cancelar`** en cualquier pregunta para abandonar la operación y volver al menú.
- Los datos de otra tabla (grados, categorías, clubes, préstamos) se eligen de una **lista numerada**, así no hay que memorizar ids.
- Las fechas se escriben `dd/MM/yyyy`; muchas tienen valor por defecto con **Enter** (hoy).
- Los valores en pesos se muestran con formato colombiano (`$ 1.500.000,00`).

---

## 14. Decisiones de diseño

- **Dinero con `BigDecimal`** y notas con una escala fija: nunca `double`, para evitar errores de redondeo.
- **No se borra información**: estudiantes, profesores, clubes y recursos cambian de estado. Un recurso dado de baja o un estudiante retirado conserva su historial.
- **Un pago, una factura**, con número consecutivo (`REC-%06d` para recibo y `LH-%06d` para factura).
- **Los préstamos se identifican por tipo e id** porque estudiantes y profesores tienen tablas de préstamos separadas cuyos ids se repiten.
- **Estadísticas en memoria con Streams** sobre listas que ya entregan los DAOs. Con el tamaño de un colegio es suficiente y deja la lógica en Java, donde se puede explicar.
- **Código, comentarios y commits en inglés**; mensajes al usuario en español; mensajes del log en inglés.

---

## 15. Problemas conocidos

1. **Las facturas son archivos TXT simulados.** No tienen validez ante la DIAN: el CUFE, la resolución y el texto QR siguen la estructura de una factura electrónica real, pero son de ejemplo. No hay PDF.
2. **No hay autenticación.** Cualquier persona que abra el programa accede a todos los módulos; no existen usuarios ni contraseñas.
3. **No hay pruebas automatizadas.** El sistema se probó manualmente, módulo por módulo, contra una base de datos real, pero no se incluyeron pruebas unitarias.
4. **Una sola conexión a la vez**: la aplicación es de consola y de un solo usuario. Los bloqueos de fila protegen los casos críticos, pero no se probó con varios usuarios simultáneos.
5. **Los reportes cargan listas completas en memoria.** Es adecuado para el tamaño del colegio, pero no escalaría a cientos de miles de registros.
6. **Las carpetas `invoices/` y `reports/` y el archivo `error_log.txt` se crean en el directorio de trabajo**, por lo que hay que ejecutar el programa desde la raíz del proyecto.
7. **Requiere MySQL 8.0 o superior.** No se probó en versiones anteriores ni en MariaDB.

---

## 16. Control de versiones y cumplimiento de la rúbrica

- Repositorio privado, rama principal `main`, con el instructor agregado como colaborador.
- Commits pequeños y atómicos, uno por grupo de archivos relacionados, con mensajes en inglés en modo imperativo (`Add resource and loan controllers...`).
- `.gitignore` excluye `out/`, `invoices/`, `reports/`, `error_log.txt` y `db.properties`.

### Dónde está cada requisito

| Requisito | Dónde verlo |
|---|---|
| Arquitectura MVC | Paquetes `model`, `view`, `controller`, `dao` (sección 5) |
| SOLID | Sección 7 (`ConnectionProvider`, `BaseDao`, `ErpException`, `BorrowerType`) |
| Patrones de diseño | Sección 8 (`AppLogger`, `DatabaseConnection`, `BaseDao.inTransaction`) |
| Lambdas y Streams | Sección 9 (`ReportController`, `LoanController`, `PaymentController`) |
| Archivos y logs | `util/AppLogger`, `util/InvoiceFileWriter`, `util/ReportFileWriter` (sección 11) |
| JDBC y PreparedStatement | `dao/BaseDao` y todos los DAOs (sección 12) |
| DML | `INSERT`, `UPDATE` y `SELECT` con `JOIN` y `GROUP BY` en los DAOs |
| Interfaz de consola | Paquete `view` (sección 13) |
| Manejo de excepciones | Paquete `exception` (sección 10) |
| Documentación y repositorio | Este archivo, `db.properties.example`, `.gitignore` |