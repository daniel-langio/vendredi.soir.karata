# Repository Guidelines & AI Context

This file defines the strict architectural and operational rules for this repository. All code modifications, implementation plans, and automation workflows must adhere to these constraints.

## 1. Core Architecture & Design Principles

*   **Pure Java Core (`core` package):** The `core` package must remain a pure Java package containing the project's core business logic (*métier pur*).
    *   **Zero Framework Dependencies:** Do not import Spring, JPA, Hibernate, or any other external infrastructure frameworks inside this package. It must rely solely on standard Java libraries.
    *   **Domain Isolation:** All core business rules, domain entities, and pure logic reside here, ensuring the application's *métier* remains highly testable and completely decoupled from technical implementation details.

*   **Strict Responsibility Separation:** Maintain clean boundaries across all layers.
    *   **Controllers:** Handle HTTP routing, input validation, and request/response mapping only. No business logic.
    *   **Services / Infrastructure:** Bridge the pure Java `core` logic with external frameworks, managing transactions, persistence, and external APIs.
    *   **Data Layer:** Isolate database operations. Maintain distinct separations between database entities (JPA) and Data Transfer Objects (DTOs) used at the API boundary.

## 2. Development Workflow & Automation
*   **Pre-push Formatting:** The local formatting script must be executed prior to pushing any changes to the remote repository. Always run:
    ```bash
    ./format
    ```
*   **Documentation Management:** For every modification, evaluate the impact on codebase documentation. Update the relevant markdown files inside the `docs/` directory immediately if the changes alter existing behavior, APIs, or setups.

## 3. Git & Pull Request Conventions
*   **Conventional Commits:** All commit messages and Pull Request titles must strictly follow the Conventional Commits specification.
    *   *Format:* `<type>(<scope>): <description>`
    *   *Allowed Types:* `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`
    *   *Examples:*
        *   `feat(controller): add endpoints for financial asset simulation`
        *   `fix(jpa): resolve mapping mismatch on entity relationships`