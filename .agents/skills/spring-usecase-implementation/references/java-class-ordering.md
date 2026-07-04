# Java Class Ordering

## Core Principle

Classes should read from top to bottom as the main flow followed by the implementation detail needed to understand that flow.

## Member Order

1. Constants
2. Fields
3. Constructors
4. Static factory methods
5. Public core method
6. Private helpers first called by that public method
7. Next public core method
8. Private helpers first called by that public method
9. Getters
10. Setters, avoided by default

## Private Helpers

- Place a private helper directly under the public method that first calls it.
- If a helper is shared by multiple public methods, still place it under the first public method that calls it.
- A shared helper may stay near the bottom only when it is broad, not usecase-specific, and placing it under the first method would make it look like a private detail of that method only.

## Public Method Order

Service methods usually follow usecase flow:

1. Create
2. Read
3. Update
4. Delete
5. Other commands

Domain methods should show behavior before state exposure. Put core behavior such as `isPast`, `isOwnedBy`, `changeTime`, or `cancel` before getters.

## Getter And Setter Policy

- Put getters after behavior and public usecase methods.
- Avoid setters.
- When state changes are needed, use domain-meaningful methods such as `changeTime(time)` rather than `setTime(time)`.

## Overloads

Keep overloaded methods adjacent.
