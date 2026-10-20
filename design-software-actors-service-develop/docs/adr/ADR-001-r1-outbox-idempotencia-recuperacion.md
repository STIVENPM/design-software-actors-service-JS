# ADR-001: Entrega fiable de eventos de Actors

**Estado:** aceptada para R1 local · **Fecha:** 2026-10-20 · **Responsable:** José Stiven Perdomo Martínez

## Contexto

Actors confirmaba cambios de disponibilidad y matrícula en PostgreSQL y enviaba eventos directamente a Kafka. Si el broker fallaba entre esas acciones, el cambio quedaba persistido sin un evento recuperable. Actors también consume eventos de IAM por RabbitMQ; una entrega repetida puede ocurrir tras reinicios o reintentos.

## Decisión

1. La escritura de disponibilidad/matrícula y el registro de `actors_tracking.outbox_event` comparten la misma transacción PostgreSQL. Sin transacción activa, se rechaza el registro del evento.
2. Un despachador reclama filas con `FOR UPDATE SKIP LOCKED`, asigna un lease de 45 s y publica el envelope en `actors-events`. Tras confirmación del broker marca `PUBLISHED`. Un proceso caído deja el lease recuperable.
3. Fallas temporales usan reintentos con espera exponencial de 1, 4, 16 y 64 s. Después de cinco intentos queda `DEAD`. Una operación protegida permite reactivar un evento muerto, con intervención explícita.
4. El consumidor IAM existente registra `event_id` en su inbox, que tiene clave primaria única, para evitar repetir efectos locales. Su contenedor RabbitMQ reintenta y conduce el mensaje a DLQ; una operación interna protegida reproduce un mensaje de la DLQ mediante transacción AMQP.
5. Kafka se ejecuta sin puerto publicado al host en Docker Infra. Los endpoints operativos se dejan fuera del gateway público predeterminado.

## Garantías y límites

- Se garantiza persistencia atómica del dato de negocio y del evento pendiente; la entrega del outbox es **al menos una vez**.
- Una caída después de la confirmación Kafka y antes de marcar `PUBLISHED` puede volver a publicar el mismo `event_id`. Los consumidores de `actors-events` deben deduplicar por `event_id`; esta implementación no puede garantizar por sí sola un único efecto en servicios externos.
- El inbox IAM evita repetir efectos en Actors para un mismo `event_id`. La capacidad de replay de DLQ requiere un operador autorizado y diagnóstico previo del mensaje.
- El endpoint de operaciones usa actualmente el permiso `ACT_INSTRUCTOR_MANAGE` porque no existe un grant operativo especializado; debe restringirse con un permiso de operaciones antes de exponerlo fuera de loopback o del entorno local.

## Alternativas consideradas

- Publicación directa después del commit: puede perder eventos si cae el proceso.
- Transacción distribuida PostgreSQL/Kafka: añade coordinación y complejidad operativa no disponible en el stack.
- Reintentos solo en memoria: pierden pendientes al reiniciar.

## Impacto

Migración y grants en `actors-db`; writer, dispatcher, operaciones y pruebas en `actors-service`; broker Kafka interno en `docker-infra`. El MFE y el shell no cambian por R1.
