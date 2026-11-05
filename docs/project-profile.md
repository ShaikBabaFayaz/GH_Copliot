# Project Technology Profile
**Project:** Inventory Management System
**Status:** Confirmed by the user on 2026-11-03; authoritative for subsequent SDLC stages.

| Area | Confirmed choice |
|---|---|
| Application | Full-stack web application |
| Backend | Java with Spring Boot |
| Frontend | React with TypeScript |
| Database | In-memory H2; data is ephemeral across process restarts |
| Deployment | Local |
| Product price policy | INR; exactly two fractional digits; reject greater precision without rounding |
| Product data policy | Seeded extensible category lookup; name 1-120 code points; SKU 1-64 normalized uppercase; description optional/max 2,000; integer countable stock max 999,999,999,999 |
| API/security policy | `/api/v1`; Problem Details; Basic auth, INVENTORY_MANAGER role; environment-provisioned credentials; loopback HTTP or network HTTPS |
| Backend toolchain | Java 21.0.12.1; Spring Boot 4.1.2; Maven Wrapper 3.3.4/Maven 3.10.0; H2 2.4.240; T03-T06 persistence, auth, product and stock service tasks complete |
| Frontend toolchain | Node 26.13.0/npm 11.20.0; React/ReactDOM 19.3.1; TypeScript 6.0.3; Vite 8.3.5; React Router 8.5.1; T02 scaffold builds |
| Test toolchain | Spring Boot Test/JUnit 6.0.3; Vitest 5.1.0 and Testing Library/jsdom pins. Backend T03-T06 suite: 25 tests passed, 0 failures. |
| UI/quality policy | WCAG 2.2 AA; 360/768/1024/1440 px; IBM Plex Sans; neutral surfaces/teal actions; provisional p95 <=500 ms at <=1,000 products |

The repository initially had no application source, manifests, or framework configuration. T02 manifests/scaffold and approved dependencies are present; backend compile and frontend production build passed. T03-T06 backend tests pass 25/25, including concurrent version conflict and atomic rollback. No unapproved dependencies installed.