---
name: holistic-ui-wiring
description: >-
  Analyzes the cascading impact of UI button changes before implementing them. Traces logic, state, and connections to other parts of the app to anticipate side effects and missing pieces.
---

# Holistic UI Wiring

## Overview
This skill enforces a strict protocol for analyzing UI changes before writing any code. When asked to modify or repurpose a UI button, the agent must trace its logic, discover its dependencies, and anticipate side effects (like redundant menus or disconnected flows) to propose a holistic solution.

## Dependencies
None. This skill orchestrates built-in code search and reasoning tools.

## Workflow

### 1. Locate the UI Element
- Search the file provided by the user using `grep_search` or `view_file` to find the exact implementation of the UI button.
- If the button is not found in the specified file, automatically expand the search to the entire codebase using `grep_search` to locate it.

### 2. Trace the Blast Radius
- Analyze what state or functionality the button currently affects (e.g., toggles a boolean, navigates to a screen, triggers an API).
- Search the codebase to see what *other* UI elements or logic rely on that same state.
- Analyze what the button *should* affect based on the user's intent.

### 3. Anticipate Side Effects
- Identify if changing the button's behavior breaks existing workflows. 
- Example: If an overflow menu item changes from a toggle to an onboarding redirect, does the label "Enable/Disable" still make sense? Should the item disappear after onboarding? If it disappears, where will the user toggle the feature moving forward?
- Determine if new UI elements need to be added to support the updated flow.

### 4. Propose and Consult
- Instead of immediately editing the code, formulate your findings.
- Present a clear summary of what needs to change, including any necessary side-effect changes you discovered.
- If decisions need to be made (e.g., "Do you want me to add a new toggle button on the main screen since we removed it from the menu?"), ask the user for guidance in a direct response. Do not use an artifact for this unless the proposed change is massive.

## Common Mistakes
- **Writing code too early:** Do not start replacing file contents until you have fully traced the button's logic and resolved side effects with the user.
- **Ignoring orphaned logic:** Failing to relocate functionality when a button's purpose changes (e.g., removing a setting from a menu without putting it somewhere else).
- **Failing loudly when a file is wrong:** If the user gives the wrong file name for the button, use your search tools to find it instead of stopping.
