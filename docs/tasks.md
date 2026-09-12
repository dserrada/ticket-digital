# Ticket Digital Improvement Tasks

This document contains a detailed list of actionable improvement tasks for the Ticket Digital project. Each task is marked with a checkbox [ ] that can be checked off when completed.

## Documentation Improvements

1. [ ] Enhance README.md with comprehensive project description, setup instructions, and usage examples
2. [ ] Add Javadoc comments to all classes and methods that are missing documentation
3. [ ] Create a user guide explaining how to use the application
4. [ ] Document the ticket format specification in a structured way
5. [ ] Add license information to all source files

## Architecture Improvements

6. [ ] Implement a proper layered architecture (e.g., presentation, service, repository)
7. [ ] Create interfaces for key components to improve testability and maintainability
8. [ ] Implement a proper dependency injection mechanism
9. [ ] Separate parsing logic from business logic
10. [ ] Create a proper domain model that accurately represents the business entities

## Code Quality Improvements

<!-- Los puntos 11, 15, 16 y 17 de esta lista mencionaban Articulo.java, un record Cantidad, y
     los métodos calcularPrecioTotalArticulos/precioSumaEnEuros: ese modelo ya no existe (se
     sustituyó por el sealed interface PurchasedItem y sus records OneItemByUnit/NItemsByUnit/
     ItemByWeight/FreshItemByWeight en ticket-digital-parser, con validación en sus constructores
     compactos y JavaDoc), así que se han retirado por no aplicar al código actual. -->
12. [ ] Replace System.out.println statements with proper logging
13. [ ] Add input validation to all public methods
14. [ ] Implement proper error handling with custom exceptions
18. [ ] Fix inconsistency in parameter naming (precioUnitarioEnCentimos vs euros)
19. [ ] Remove FIXME comments and implement proper solutions
20. [ ] Add null checks to prevent NullPointerExceptions

## Testing Improvements

21. [ ] Increase test coverage for all classes
22. [ ] Add unit tests for edge cases
23. [ ] Implement integration tests for the ticket parsing functionality
24. [ ] Add parameterized tests for different ticket formats
25. [ ] Create test utilities to simplify test setup

## Feature Enhancements

26. [ ] Implement support for additional quantity types (e.g., liters)
27. [ ] Add functionality to export tickets to different formats (e.g., PDF, CSV)
28. [ ] Implement a command-line interface for the application
29. [ ] Add support for parsing tickets from different supermarkets
30. [ ] Implement functionality to calculate tax information

## Build and CI/CD Improvements

31. [ ] Configure Gradle to generate proper JavaDoc
32. [ ] Add code quality plugins (e.g., Checkstyle, PMD, SpotBugs)
33. [ ] Set up continuous integration with GitHub Actions or similar
34. [ ] Configure code coverage reporting
35. [ ] Implement automated release process

## Performance Improvements

36. [ ] Optimize ticket parsing algorithm for large tickets
37. [ ] Implement caching for frequently accessed data
38. [ ] Use StringBuilder instead of string concatenation in performance-critical sections
39. [ ] Review and optimize memory usage
40. [ ] Implement batch processing for handling multiple tickets

## Security Improvements

41. [ ] Review code for potential security vulnerabilities
42. [ ] Implement input sanitization for all user inputs
43. [ ] Add secure handling of sensitive information (e.g., payment details)
44. [ ] Implement proper authentication and authorization if needed
45. [ ] Add security headers for web interfaces if applicable

## Ideas en estudio (no decididas)

Estas son ideas que se están valorando, sin decisión tomada todavía sobre si merece la
pena implementarlas. Se documentan aquí solo para no perderlas.

46. [ ] **Evitar que cada usuario tenga que crear su propio proyecto en Google Cloud
    Console para obtener `credentials.json`.** No es posible eliminar por completo el
    proyecto de Google Cloud: todo flujo OAuth (incluida cualquier pantalla de "autorizar
    esta app" en la que Google redirige al usuario) necesita un `client_id`/`client_secret`
    registrado que identifique a la aplicación que pide acceso — no hay forma de que
    Google muestre esa pantalla de consentimiento sin una app registrada detrás. Lo que sí
    se podría hacer es mover ese paso de "cada usuario crea su proyecto" a "el
    mantenedor del proyecto crea uno y distribuye su `client_id`/`client_secret` junto con
    la aplicación" (`ticket-digital-tickets-downloader` ya usa
    `AuthorizationCodeInstalledApp`/`LocalServerReceiver`, que ya implementan el
    redirigir al navegador y recibir la respuesta sin que el usuario copie ningún
    código a mano; solo haría falta dejar de exigir `--credentials-file` y empotrar esas
    credenciales). El problema de fondo: `gmail.readonly` es un "scope restringido" de
    Google, así que si la app la usan más de ~100 usuarios, Google exige pasar su proceso
    de verificación de apps OAuth (incluida una revisión de seguridad) o se queda
    limitada a 100 usuarios de prueba añadidos a mano en la consola, con una pantalla de
    aviso de "app no verificada" para el resto — viable para uso personal o de un grupo
    pequeño, no para distribución pública amplia sin pasar por esa verificación.