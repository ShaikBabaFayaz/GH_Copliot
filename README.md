# Inventory Management System

A local, single-location inventory application for managing products, recording stock movements, reviewing movement history, monitoring low stock, and searching/filtering the product catalog.

## Features

- Product creation, editing, search, category/status filters, and terminal deactivation.
- Stock IN/OUT with positive integer quantities, reason/reference, actor audit, and optimistic version checks.
- Chronological movement history retained after product deactivation.
- Low-stock monitoring when quantity is less than or equal to the configured threshold, including zero.
- HTTP Basic authentication for the `INVENTORY_MANAGER` role.

Barcode scanning, suppliers, purchase orders, and multi-warehouse inventory are out of scope.

## Technology

- Backend: Java 21, Spring Boot 4.1.2, Maven Wrapper, H2.
- Frontend: React 19.3.1, TypeScript 6.0.3, Vite 8.3.5, React Router 8.5.1.
- Tests: Spring Boot Test with BOM-managed JUnit Jupiter 6.0.3; Vitest 5.1.0 and Testing Library.

## Prerequisites

- JDK 21.
- Node.js 26.13.0 and npm 11.20.0.
- PowerShell on Windows. The Maven Wrapper downloads the configured Maven distribution on first use.

## Run Locally (Windows)

1. Open a PowerShell terminal in `backend` and set credentials for this terminal session. Choose your own values; do not commit them or put them in project files.

   ```powershell
   $env:INVENTORY_MANAGER_USERNAME = "your-local-manager-name"
   $env:INVENTORY_MANAGER_PASSWORD = "your-local-password"
   .\mvnw.cmd spring-boot:run
   ```

   The backend listens on `http://127.0.0.1:8080`. All API routes require Basic authentication. Keep the process running.

2. Open a second PowerShell terminal in `frontend` and install the locked packages, then start Vite:

   ```powershell
   npm.cmd ci
   npm.cmd run dev
   ```

3. Open `http://127.0.0.1:5173` in your browser and sign in with the username and password set in the backend terminal.

The Vite development server proxies `/api` requests to the local backend. Stop either process with `Ctrl+C`.

## Data and Security Notes

- H2 runs in memory. Products and movement history are cleared when the backend process restarts.
- Credentials are required from environment variables, hashed in memory, and are not given default values.
- The backend binds to loopback by default. Do not expose it on a network without configuring HTTPS and reviewing the security requirements.

## Tests and Build

Backend tests use the Maven Wrapper. Set the two required environment variables in the same PowerShell session, then run from `backend`:

```powershell
$env:INVENTORY_MANAGER_USERNAME = "your-local-manager-name"
$env:INVENTORY_MANAGER_PASSWORD = "your-local-password"
.\mvnw.cmd test
```

Frontend tests and production build, from `frontend`:

```powershell
npm.cmd test
npm.cmd run build
```

See `docs/requirements.md`, `docs/architecture.md`, `docs/ui-ux-spec.md`, and `docs/implementation-plan.md` for approved behavior, constraints, and acceptance criteria. The performance target remains provisional and has not been benchmarked.
