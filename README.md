# Ski Lift Analytics

This project computes skier vertical gain, lift ride counts, and the ten busiest lifts per operational hour from a CSV file. It has sequential and concurrent analyzers. Spring Boot exposes the same analysis through a small HTTP API.

## Build and run

Requires JDK 17 or newer. The Gradle wrapper uses Gradle 8.14.

```sh
./gradlew build
./gradlew bootRun
```

To run the packaged application instead, use `java -jar build/libs/ski-lift-analytics-1.0.0.jar`.

The bundled input is `src/main/resources/ski_lift_data.csv` (800,000 rides). The CLI writes `Skiers.csv`, `Lifts.csv`, and `Hours.csv` under the ignored `out/` directory by default. To use another input or output directory:

```sh
./gradlew classes
java -cp 'build/classes/java/main:build/resources/main' skiliftanalytics.concurrent.SkiDataProcessor concurrent /path/to/rides.csv out/concurrent
java -cp 'build/classes/java/main:build/resources/main' skiliftanalytics.concurrent.SkiDataProcessor sequential /path/to/rides.csv out/sequential
```

`SkiDataProcessor [concurrent|sequential] [input.csv] [output-dir]` defaults to concurrent mode and the bundled input. The `skiliftanalytics.sequential.Main [input.csv] [output-dir]` entry point also runs sequential analysis. CLI elapsed time includes input reading, analysis, and report writing.

## HTTP API

With `bootRun` running on port 8080:

```sh
curl http://localhost:8080/api/health
curl -X POST http://localhost:8080/api/analysis/run -H 'Content-Type: application/json' -d '{"mode":"sequential"}'
curl -X POST http://localhost:8080/api/analysis/upload -F 'file=@/path/to/rides.csv' -F 'mode=concurrent'
```

Both analysis endpoints accept `sequential` or `concurrent`; omitting the mode selects concurrent. `/run` analyzes the bundled CSV, while `/upload` analyzes the uploaded CSV (maximum 50 MB). The JSON response contains a summary, the top 100 skiers, all used lifts, and up to ten lifts per hour. `summary.durationMs` measures analysis after CSV reading and before JSON mapping or HTTP transfer.

Skier totals sort by vertical gain descending, then skier ID ascending as text; exactly the first 100 are returned. Lift totals sort by lift ID ascending as text. Hourly results sort by hour ascending, then ride count descending and lift ID ascending as text; exactly the first ten lifts per hour are returned. Ties do not expand either limit.

## CSV contract and failures

Use five comma-separated columns in this order: `resort,day,skier,lift,time`. The header `Resort,Day,SkierID,LiftID,Time` is also accepted. Resort, day, and skier are nonblank string IDs. Lift is a canonical integer from 1 through 40. Time is an integer from 0 through 360 minutes; minutes 0–59 are hour 0, and minute 360 is included in hour 5. Lift ranges 1–10, 11–20, 21–30, and 31–40 contribute 200, 300, 400, and 500 vertical units per ride, respectively.

A header-only CSV produces empty statistics. An empty file, wrong header, wrong field count, blank required value, invalid number, or out-of-range value fails with a row number and reason; bad rows are not silently skipped. Invalid input and mode values return HTTP 400 (`BAD_REQUEST`). Input read failures return HTTP 500 (`INPUT_READ_FAILED`), analysis failures return HTTP 500 (`ANALYSIS_FAILED`), and interruptions return HTTP 503 (`ANALYSIS_INTERRUPTED`). The CLI also fails instead of reporting success for invalid input or analysis failure. The parser expects simple unquoted fields; embedded commas are not supported.

## Performance comparison

Measured on the bundled 800,000-row (13.8 MB) CSV using a MacBook Pro with Apple M4 Pro (14 CPU cores, 24 GB RAM) and Java 21.0.8. In one running Spring Boot process, send alternating `POST /api/analysis/run` requests for each mode, discard two warmups per mode, then record five requests per mode. Compare `topSkiers`, `liftUsage`, and `hourlyTopLifts` before comparing times; all three arrays matched on every run.

| Mode | `summary.durationMs` (five runs) | Median | Local HTTP wall-time median |
| --- | --- | ---: | ---: |
| Sequential | 254, 229, 345, 230, 212 ms | 230 ms | 383 ms |
| Concurrent | 743, 687, 1081, 1122, 722 ms | 743 ms | 912 ms |

`summary.durationMs` starts after CSV reading and ends before JSON mapping; it includes grouping or queue processing, aggregation, sorting, and thread lifecycle. Local HTTP wall time also includes CSV reading, JSON serialization, and transfer. The concurrent mode was slower on this workload, so this is a reliability and trade-off demonstration rather than a speedup claim. Results vary with JVM warmup, garbage collection, and machine load.
