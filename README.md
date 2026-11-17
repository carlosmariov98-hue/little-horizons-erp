# ERP Colegio Little Horizons

Sistema de planificación de recursos empresariales (ERP) educativo, por consola, para el Colegio Internacional Little Horizons. Hecho en **Java 17**, **MySQL 8** y **JDBC puro**, con arquitectura **MVC**.

## Tabla de contenido

1. [Título del Proyecto](#1-título-del-proyecto)
2. [Descripción](#2-descripción)
3. [Autores](#3-autores)
4. [Tecnologías Utilizadas](#4-tecnologías-utilizadas)
5. [Requerimientos Previos](#5-requerimientos-previos)
6. [Configuración de la Base de Datos](#6-configuración-de-la-base-de-datos)
7. [Instalación y Ejecución](#7-instalación-y-ejecución)
8. [Estructura del Proyecto](#8-estructura-del-proyecto)
9. [Principios y Patrones de Diseño Aplicados](#9-principios-y-patrones-de-diseño-aplicados)
10. [Funcionalidades Principales](#10-funcionalidades-principales)
11. [Manejo de Errores y Logs](#11-manejo-de-errores-y-logs)
12. [Capturas de Pantalla](#12-capturas-de-pantalla)
13. [Problemas Conocidos y Limitaciones](#13-problemas-conocidos-y-limitaciones)
14. [Futuras Mejoras](#14-futuras-mejoras)
15. [Licencia](#15-licencia)
16. [Contacto](#16-contacto)

---

## 1. Título del Proyecto

**ERP Colegio Little Horizons**: Sistema de Gestión Educativa.

---

## 2. Descripción

### Propósito

El Colegio Internacional "Little Horizons" atiende a estudiantes desde preescolar hasta grado 11°. Este sistema centraliza y automatiza sus procesos académicos, administrativos y financieros en una sola aplicación conectada a una base de datos MySQL.

### Problemas que resuelve

| Problema del colegio | Cómo lo resuelve el sistema |
|---|---|
| Pérdida de información académica (notas en cuadernos) | Las notas, matrículas e historial viven en la base de datos, con consulta del historial completo de cada estudiante. |
| Actividades extracurriculares sin registro | Catálogo de clubes con cupo, afiliaciones con seguimiento y eventos con participantes y posiciones. |
| Facturación manual y propensa a errores | Cada pago genera automáticamente su factura electrónica (simulada) en un archivo de texto, con IVA y CUFE. |
| Libros y equipos que se "pierden" | Inventario de recursos y préstamos que registran quién tiene cada cosa y detectan los vencidos. |
| Falta de visibilidad sobre el rendimiento | Reportes de promedios, estudiantes en riesgo, asistencia, morosos y ocupación de clubes. |

### Funcionalidades principales

Estudiantes y acudientes, profesores y carga académica, matrículas, notas con promedios automáticos, asistencia, clubes, eventos con podio, conceptos de pago, pagos con factura, morosos, inventario de recursos, préstamos y reportes. El detalle está en la [sección 10](#10-funcionalidades-principales).

---

## 3. Autores

| Campo | Dato                                                      |
|---|-----------------------------------------------------------|
| Nombre completo | Carlos Mario Velasquez Angulo                             |
| Correo electrónico | carlosmariov98@gmail.com                                  |
| Contacto | https://github.com/carlosmariov98-hue/little-horizons-erp |

---

## 4. Tecnologías Utilizadas

| Elemento | Detalle |
|---|---|
| Lenguaje | Java 17 |
| IDE | IntelliJ IDEA (proyecto Java estándar, sin Maven ni Gradle) |
| Base de datos | MySQL 8.0 o superior |
| Librería externa | **Una sola**: MySQL Connector/J 9.7.0 (`lib/`) |
| Frameworks | Ninguno |

El resto sale de la biblioteca estándar de Java: `java.sql` (JDBC), `java.time` (fechas), `java.math.BigDecimal` (dinero y notas), `java.security.MessageDigest` (CUFE), `java.nio.file` (archivos) y `java.util.stream` (estadísticas).

---

## 5. Requerimientos Previos

| Software | Versión |
|---|---|
| JDK | 17 o superior |
| **MySQL** | **8.0 o superior** (se requiere 8.0.16 o más, porque el esquema usa restricciones `CHECK`) |
| MySQL Connector/J | 9.7.0 (el `.jar` está en `lib/`) |
| Git | Cualquier versión reciente |
| IDE | IntelliJ IDEA (recomendado) o cualquier IDE Java |

---

## 6. Configuración de la Base de Datos

### 6.1. Creación de la base de datos

El archivo `docs/database.sql` tiene dos secciones: **DDL** (estructura completa de `erp_little_horizons`, con llaves, índices y `CHECK`) y **DML** (datos de prueba coherentes, con más de 30 registros en cada tabla principal). Se ejecuta de una sola vez:

```
mysql -u root -p --default-character-set=utf8mb4 < docs/database.sql
```

El script crea la base, las tablas y los datos. También puede abrirse en MySQL Workbench y ejecutarse completo.

### 6.2. Configuración de credenciales

Copie `db.properties.example` como `db.properties` en la raíz del proyecto y escriba sus datos:

```
db.url=jdbc:mysql://localhost:3306/erp_little_horizons
db.user=su_usuario
db.password=su_clave
```

`db.properties` está en `.gitignore`: las credenciales nunca se suben al repositorio. Si el archivo no existe, el programa explica cómo crearlo.

### 6.3. Instalación del driver JDBC

El `.jar` de MySQL Connector/J está en `lib/`. En IntelliJ: *File > Project Structure > Libraries > + > Java*, seleccione el `.jar` de `lib/` y acepte.

---

## 7. Instalación y Ejecución

### 7.1. Clonar el repositorio

```
git clone https://github.com/carlosmariov98-hue/little-horizons-erp.git
cd little-horizons-erp
```

### 7.2. Abrir el proyecto en el IDE

*File > Open* y seleccione la carpeta del proyecto. Verifique que el SDK sea Java 17 y que el driver de `lib/` esté agregado (sección 6.3).

### 7.3. Compilar

En IntelliJ: *Build > Build Project*. Por consola, desde la raíz del proyecto (Linux/macOS):

```
javac -encoding UTF-8 -cp "lib/*" -d out $(find src -name "*.java")
```

En Windows, liste primero los archivos: `dir /s /b src\*.java > sources.txt` y compile con `javac -encoding UTF-8 -cp "lib/*" -d out @sources.txt`.

### 7.4. Ejecutar

Ejecute la clase `com.littlehorizons.erp.Main` desde IntelliJ (el directorio de trabajo debe ser la raíz del proyecto, porque ahí se lee `db.properties` y se crean `error_log.txt`, `invoices/` y `reports/`). Por consola:

```
java -cp "out:lib/*" com.littlehorizons.erp.Main        (en Windows: "out;lib/*")
```

Si MySQL no está encendido o `db.properties` no existe, el programa muestra un mensaje claro en español y termina sin mostrar errores técnicos.

---

## 8. Estructura del Proyecto

```
little-horizons-erp/
├── docs/
│   └── database.sql              DDL + DML (datos de prueba)
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
│   └── util/                     Validator, AppLogger, archivos de factura y reporte, formato de dinero y fechas
├── db.properties.example         plantilla de configuración
├── .gitignore
└── README.md
```

### Arquitectura MVC

```
 Usuario ──> view (menús)  ──>  controller (reglas)  ──>  dao (SQL)  ──>  MySQL
              lee y muestra       valida y decide          JDBC
                  ▲                    │
                  └──── model ◄────────┘   (objetos que viajan entre capas)
```

| Paquete | Responsabilidad | Lo que NO hace |
|---|---|---|
| `model` | Guarda los datos (entidades y enums). | No accede a la base ni imprime. |
| `view` | Muestra menús, lee lo que escribe el usuario y presenta resultados. | No contiene reglas de negocio ni SQL. |
| `controller` | Valida, aplica las reglas de negocio y coordina los DAOs. | No imprime ni lee consola. |
| `dao` | Ejecuta el SQL con `PreparedStatement` y convierte filas en objetos. | No decide reglas de negocio (salvo las que deben ir dentro de una transacción). |
| `config` | Lee `db.properties` y entrega conexiones. | No conoce el resto del sistema. |
| `exception` | Define los errores del sistema. | |
| `util` | Utilidades compartidas: validación, log, fechas, dinero, archivos. | |

Los objetos se conectan por constructor en `Main.java`, que es el único lugar que conoce todas las clases concretas.

---

## 9. Principios y Patrones de Diseño Aplicados

### 9.1. Principios SOLID

**S, Responsabilidad única.** Cada clase hace una sola cosa. `AppConfig` solo lee configuración; `DatabaseConnection` solo entrega conexiones; `CufeGenerator` solo calcula el CUFE; `TaxPolicy` solo decide el IVA; `InvoiceFileWriter` solo escribe el archivo de la factura; `ReportFileWriter` solo escribe el de los reportes; `Validator` solo valida. Los menús solo hablan con el usuario, los controllers solo aplican reglas y los DAOs solo ejecutan SQL.

**O, Abierto a extensión, cerrado a modificación.** Un DAO nuevo se crea extendiendo `BaseDao` y usando sus ayudantes (`queryList`, `executeUpdate`, `inTransaction`) sin tocar la clase base. `BorrowerType` guarda las tablas y columnas de cada tipo de prestatario: agregar un tercer tipo (por ejemplo, personal administrativo) es una constante nueva y `LoanDao` no cambia.

**L, Sustitución de Liskov.** Todas las excepciones del sistema extienden `ErpException`, por eso `BaseMenu.runSafely` atrapa cualquiera y la trata igual. Todos los DAOs se usan a través de `BaseDao` y todos los menús a través de `BaseMenu`, sin que quien los llama sepa cuál es.

**I, Segregación de interfaces.** Las interfaces son mínimas y de un solo método: `ConnectionProvider`, `Binder`, `RowMapper`, `TransactionalAction`, `Parser`, `ChoiceValidator` y `MenuAction`. Nadie depende de métodos que no usa.

**D, Inversión de dependencias.** Los DAOs reciben un `ConnectionProvider` (una interfaz) y no la clase `DatabaseConnection`, así la fuente de conexiones puede cambiar sin tocar los DAOs. Los controllers reciben sus DAOs por constructor y los menús reciben sus controllers por constructor; nada se instancia dentro de las clases de negocio. Todo se ensambla en `Main`.

### 9.2. Patrones de diseño

| Patrón | Dónde | Justificación |
|---|---|---|
| **Singleton** | `AppLogger`, `DatabaseConnection` | Debe existir un solo log y una sola configuración de conexión. `AppLogger` se crea al cargar la clase; `DatabaseConnection.getInstance()` es `synchronized`. |
| **DAO** | Paquete `dao` | Aísla todo el SQL. El resto del sistema nunca ve `Connection` ni `ResultSet`, lo que facilita cambiar la base de datos. |
| **Template Method (con lambdas)** | `BaseDao.inTransaction`, `BaseMenu.runSafely` | La clase base fija los pasos que no cambian (abrir, confirmar o deshacer, cerrar; atrapar errores y volver al menú) y la lambda aporta lo variable. |
| **Strategy (con lambdas)** | `Binder`, `RowMapper`, `Parser` | Cada consulta entrega su propia forma de llenar parámetros y de leer filas, sin duplicar el código común. |
| **Inyección de dependencias** | Constructores y `Main` | Las clases reciben lo que necesitan; es más simple de entender y de cambiar. |
| **Fachada** | `ReportController` | Una entrada única que consulta y combina varios módulos para los reportes. |

### 9.3. Lambdas y Stream API

| Dónde | Qué hace | Operaciones |
|---|---|---|
| `ReportController.studentAverages` | Promedio por estudiante, de mejor a peor | `groupingBy`, `collectingAndThen`, `map`, `sorted` |
| `ReportController.subjectAverages` | Promedio por materia | `groupingBy` con `TreeMap` |
| `ReportController.levelDistribution` | Estudiantes por nivel de desempeño | `groupingBy`, `counting` |
| `ReportController.incomeByMonth` | Recaudo por mes | `groupingBy`, `reducing` |
| `ReportController.topAbsences` | Estudiantes con más ausencias | `filter`, `groupingBy`, `sorted`, `limit`, `toMap` |
| `PaymentController.getDelinquentStudents` | Morosos | `map`, `filter`, `sorted` con `nullsFirst` |
| `PaymentController.totalsByConcept` | Totales por concepto | `groupingBy`, `reducing` |
| `LoanController.getMostBorrowed` | Recursos más prestados | `groupingBy`, `counting`, `sorted`, `limit`, `toMap` |
| `LoanController.averageDaysLate` | Retraso promedio con `BigDecimal` | `filter`, `map`, `reduce` |
| `ResourceController.availableByCategory` | Disponibles por categoría | `groupingBy` con `TreeMap` |
| `EventController` | Podio y eventos por tipo | `groupingBy`, `counting`, `sorted` |
| Menús y DAOs | Acciones, validaciones y lectura de filas | Lambdas y referencias a métodos (`this::lend`, `MenuAction`, `RowMapper`) |

---

## 10. Funcionalidades Principales

### Gestión Académica

- **Estudiantes:** registro completo (nombres, documento acorde a la edad, fecha de nacimiento, dirección, contacto, tipo de sangre, alergias y condiciones médicas), edición, búsqueda y cambio de estado.
- **Acudientes:** cada estudiante debe tener **al menos uno**; un acudiente puede tener varios estudiantes (hermanos) y uno es el responsable financiero.
- **Matrícula:** por grado (Preescolar a Once) y año académico, con estado. Solo estudiantes activos, y **una matrícula por año**.
- **Inscripción en materias** por período, solo de las materias del grado del estudiante.
- **Profesores:** directorio con especialización y contacto, y asignación de materias por año y sección (solo profesores activos, sin duplicados).
- **Notas:** de 0.0 a 5.0 con un decimal, por materia y período (I a IV y Final). La nota **Final se calcula sola** con los cuatro períodos. Solo un profesor activo y asignado a esa materia califica.
- **Promedios automáticos:** por materia de cada estudiante, general del estudiante por período y del curso por materia, más el nivel de desempeño (Bajo, Básico, Alto, Superior, Decreto 1290).
- **Historial académico** completo de cada estudiante.
- **Asistencia diaria** por materia (Presente, Ausente, Tardanza, Excusa Médica), solo de lunes a viernes, con **reporte de estudiantes con más de 3 ausencias en la semana**.

### Actividades Extracurriculares

- **Clubes:** catálogo con descripción, cupo máximo y profesor encargado. El cupo se valida dentro de una transacción con bloqueo de fila, así dos inscripciones simultáneas no superan el máximo. Un estudiante puede estar en varios clubes; al retirarse y volver se reactiva su afiliación. Cada afiliación guarda fecha de ingreso y estado (Activo, Retirado).
- **Eventos:** calendario con nombre, fecha, descripción y tipo (Académico, Cultural, Deportivo, Social); participantes; en eventos con competencia, posición (1er, 2do, 3er lugar o Participante) y podio.

### Gestión Financiera

- **Conceptos de pago** con valor base (matrícula, pensión, uniforme, actividades, etc.).
- **Valor personalizado por estudiante:** al registrar el pago el monto puede ajustarse hacia abajo (descuento por hermanos, beca parcial) y no puede superar el valor base.
- **Pagos** con estudiante, concepto, monto, fecha, método (Efectivo, Transferencia, Tarjeta) y número de recibo; historial por estudiante o por acudiente.
- **Factura electrónica (simulada) en archivo de texto** al confirmar el pago: razón social, NIT, dirección, teléfono y correo del colegio; datos del acudiente; número consecutivo (`LH-000123`); fecha; detalle con concepto, cantidad y valor; IVA (19% en uniformes y certificados); total; **CUFE** (hash SHA-384 de número de factura, NIT y fecha); texto del código QR simulado y resolución de habilitación simulada. Se guarda como `invoices/factura_000123.txt`.
- **Morosos:** estudiantes activos sin pago de pensión en los últimos 30 días.

### Gestión de Recursos

- **Inventario** con código único, nombre, categoría, estado (Disponible, Prestado, En Mantenimiento, Dado de Baja) y ubicación. *Prestado* lo asignan solo los préstamos; *Dado de Baja* es definitivo.
- **Préstamos** a estudiantes y profesores: máximo **3 abiertos por persona**, duración máxima de **60 días**, y quien tenga un préstamo vencido no puede pedir otro. El préstamo y el estado del recurso cambian juntos en una transacción.
- **Devolución:** el recurso vuelve a *Disponible* (o pasa a mantenimiento si llegó dañado) y se guarda la fecha real; las observaciones registran los días de retraso.
- **Vencidos:** se marcan automáticamente y hay un reporte con los días de retraso.

### Reportes y Estadísticas

Estudiantes por estado, género y grado; rendimiento de un grado (promedios, niveles, mejores y estudiantes en riesgo); asistencia; recaudo por concepto y por mes; morosos; ocupación de clubes; recursos más prestados; y un **resumen general** que puede guardarse en `reports/resumen_general_AAAAMMDD.txt`.

### Interfaz de consola

Menú principal agrupado por módulo, cada dato se vuelve a pedir hasta que sea válido, mensajes de éxito y error claros, listas numeradas para elegir datos relacionados y la palabra **`cancelar`** en cualquier pregunta para abandonar la operación y volver al menú.

---

## 11. Manejo de Errores y Logs

### Sistema de logging

`AppLogger` (Singleton) agrega líneas al archivo **`error_log.txt`** en la raíz del proyecto (directorio de trabajo). Solo agrega al final, nunca borra, y si no puede escribir el archivo el programa sigue funcionando.

| Nivel | Qué registra |
|---|---|
| `INFO` | Inicio y cierre de la aplicación, pagos registrados, archivos de factura y de reporte escritos, préstamos y devoluciones, operaciones canceladas. |
| `WARN` | Operaciones rechazadas por una regla de negocio o validación (con el tipo de excepción). |
| `ERROR` | Fallas técnicas: base de datos, archivos, errores inesperados, con el *stack trace* completo. |

Ejemplo real:

```
2026-11-17 13:45:51 [INFO] Application started. Database connection verified.
2026-11-17 13:45:51 [INFO] Payment registered: receipt REC-000589, invoice LH-000589, student id 1, concept MAT, amount 500000.00.
2026-11-17 13:45:51 [INFO] Invoice file written: invoices/factura_000589.txt.
2026-11-17 13:45:51 [INFO] Loan registered: resource id 1 lent to STUDENT id 1 (loan id 91).
2026-11-17 13:45:51 [WARN] Operation rejected: BusinessRuleException.
2026-11-17 13:45:51 [INFO] Application closed by the user.
```

Los mensajes del log están en inglés y los que ve el usuario en español. El detalle técnico nunca se muestra en pantalla.

### Estrategia de manejo de excepciones

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

- Los errores se lanzan donde ocurren y se atrapan en un solo lugar: `BaseMenu.runSafely`, que muestra el mensaje en español, lo registra y devuelve al usuario al menú.
- Los errores de MySQL se traducen: el código 1062 (dato duplicado) y 1452 (llave foránea) se convierten en mensajes claros.
- `OperationCancelledException` es una excepción de flujo de la interfaz, por eso vive en `view` y no extiende `ErpException`: cancelar no es un error.
- Si el archivo de una factura falla **después** de guardar el pago, el pago no se pierde: el sistema avisa y permite generar la factura de nuevo.
- Cualquier `RuntimeException` inesperado se registra con su traza y el programa muestra un mensaje en vez de caerse.

### Persistencia con JDBC

`PreparedStatement` en todas las consultas (no hay concatenación de datos del usuario), `try-with-resources` en cada `Connection`, `PreparedStatement` y `ResultSet`, transacciones con `BaseDao.inTransaction` (confirmación o deshacer automático) y bloqueo de filas con `SELECT ... FOR UPDATE` donde hay riesgo de concurrencia. Se usan `INSERT`, `UPDATE`, `SELECT` (con `JOIN`, `GROUP BY` y funciones de agregación) y `DELETE`. El `DELETE` solo se usa para retirar a un participante de un evento que aún no tiene resultado; el resto de las entidades cambian de estado, para conservar el historial.

---

## 12. Capturas de Pantalla

**Menú principal**

```
╔══════════════════════════════════════════════╗
║   ERP Colegio Little Horizons                ║
║   Sistema de Gestión Educativa               ║
╚══════════════════════════════════════════════╝

1. Gestión de Estudiantes
2. Gestión Académica
3. Gestión de Profesores
4. Actividades Extracurriculares
5. Gestión Financiera
6. Gestión de Recursos
7. Reportes y Estadísticas
8. Salir
```

**Reporte: resumen general**

```
RESUMEN GENERAL 2026
  Estudiantes activos                  46
  Profesores activos                   38
  Estudiantes matriculados en 2026     47
  Recaudo de 2026                      $ 166.205.000,00
  Estudiantes morosos                  4
  Clubes activos                       29
  Miembros activos en clubes           84
  Recursos disponibles                 31
  Recursos prestados                   13
  Préstamos vencidos                   11
```


```

```

**Factura generada (`invoices/factura_000589.txt`)**

```
================================================================
            COLEGIO LITTLE HORIZONS
      FACTURA ELECTRÓNICA DE VENTA (SIMULADA)
================================================================
NIT: 900123456-8        Régimen: Responsable de IVA
Resolución DIAN No. 18764000000001 del 2026-01-01 (simulada)
Rango autorizado: LH-000001 a LH-999999 - Vigencia: 12 meses
----------------------------------------------------------------
Factura No.:      LH-000589
Fecha de emisión: 17/11/2026
Recibo de caja:   REC-000589
Método de pago:   Efectivo
----------------------------------------------------------------
CLIENTE (ACUDIENTE)
Nombre:     Ana María Parra Rojas
Documento:  CC 1125395416
Dirección:  Avenida 36 # 35-29, Casa 7
Teléfono:   3128206804
Correo:     ana.parra@gmail.com
----------------------------------------------------------------
ESTUDIANTE
Nombre:     Isaac Gutiérrez Parra
Documento:  1188361676
----------------------------------------------------------------
DETALLE
Concepto:   Matrícula (MAT)
Año: 2026     Período: II
----------------------------------------------------------------
Valor antes de IVA                                 $ 500.000,00
IVA (servicio educativo excluido)                        $ 0,00
TOTAL PAGADO                                       $ 500.000,00
================================================================
CUFE (SHA-384):
5d4109dd5e773c680c198bbe174df37dee7e256e33dbb21f
30a2b2433d7c39f9cb11591f04d087080f4ed7c0a53df8ce
QR (simulado): https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey=5d4109dd5e773c68...
================================================================
Documento generado con fines académicos. No tiene validez fiscal.
```


---

## 13. Problemas Conocidos y Limitaciones

1. **La factura electrónica es simulada y se guarda como archivo de texto (TXT)**, por indicación del instructor (el enunciado general mencionaba PDF). El CUFE, la resolución de habilitación y el texto del código QR siguen la estructura de una factura real pero no tienen validez ante la DIAN.
2. **No hay horarios de clase.** El esquema de base de datos entregado no tiene columnas de día y hora en la asignación de profesores, por lo que el sistema asigna materias por año y sección pero no registra horarios ni detecta choques.
3. **No hay autenticación.** Cualquier persona que abra el programa accede a todos los módulos; no hay usuarios ni contraseñas.
4. **No hay pruebas automatizadas.** El sistema se probó manualmente, módulo por módulo, contra una base de datos MySQL real.
5. **Aplicación de un solo usuario.** Los bloqueos de fila protegen los casos críticos (cupos, préstamos), pero no se probó con varios usuarios simultáneos.
6. **Los reportes cargan listas en memoria.** Es adecuado para el tamaño de un colegio, pero no escalaría a cientos de miles de registros.
7. **Los archivos generados** (`error_log.txt`, `invoices/`, `reports/`) se crean en el directorio de trabajo, por lo que hay que ejecutar el programa desde la raíz del proyecto.
8. **Requiere MySQL 8.0.16 o superior.** No se probó en versiones anteriores.

---

## 14. Futuras Mejoras

- Autenticación con roles (administrador, profesor, secretaría) y contraseñas con hash.
- Registro de horarios de clase con detección de conflictos.
- Pruebas unitarias de los controllers y de integración de los DAOs.
- Interfaz gráfica o web sobre los mismos controllers (la capa de reglas ya está separada de la consola).
- Factura en PDF y facturación electrónica real con proveedor tecnológico autorizado por la DIAN.
- Envío de la factura y de las alertas de morosidad por correo electrónico.
- Respaldo automático de la base de datos.

---

## 15. Licencia

Proyecto académico desarrollado con fines educativos. El código puede ser revisado y evaluado por el instructor; cualquier otro uso requiere autorización del autor.

---

## 16. Contacto

- **Autor:** Carlos Mario Velasquez Angulo
- **Correo:** carlosmariov98@gmail.com
- **Repositorio:** https://github.com/carlosmariov98-hue/little-horizons-erp.git