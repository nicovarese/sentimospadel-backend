# Frontend / Backend Push Delivery Alignment

## Current state
- The app already has a backend-driven in-app inbox under `/api/notifications`.
- Notification consent is captured at registration and persisted on `users`:
  - `activity_tracking_enabled`
  - `operational_notifications_enabled`
- There is no current mobile packaging in the frontend repository:
  - no Capacitor
  - no Firebase messaging client
  - no APNs integration
  - no OneSignal SDK
- Because of that, the frontend cannot yet obtain native Android/iOS push tokens on its own.

## Product constraint
- Backend remains the source of truth for notification events and delivery eligibility.
- External push must never bypass:
  - user authentication
  - stored notification consent
  - persisted notification records

## Alignment decision
- Keep the existing inbox as the official notification model.
- Add a backend push-delivery layer on top of persisted notifications.
- Introduce device registration endpoints so a future mobile shell can register native tokens without changing the notification domain.
- Gate external push by `operational_notifications_enabled`.
- Keep delivery provider log-only by default in local/dev until a real provider is configured.

## Backend contract to add
- `GET /api/notifications/preferences`
  - returns the stored notification/tracking consent state for the authenticated user
- `PUT /api/notifications/preferences`
  - updates notification/tracking consent state for the authenticated user
- `POST /api/notifications/devices/register`
  - registers or refreshes a device installation for the authenticated user
- `POST /api/notifications/devices/unregister`
  - deactivates a previously registered installation for the authenticated user

## Data model to add
- `push_device_installations`
  - authenticated user ownership
  - platform
  - installation id
  - push token
  - active flag
  - last seen timestamp
- `push_notification_deliveries`
  - notification id
  - installation id
  - delivery status
  - provider name
  - provider message id
  - failure / skip reason
  - attempted / delivered timestamps

## Delivery behavior
- When a persisted notification becomes newly active and unread, backend attempts external push delivery.
- If the user opted out of operational notifications, backend records the delivery as skipped.
- If the user has no active devices, the inbox notification still exists and remains the source of truth.
- Real providers can be plugged in later without changing frontend contracts.

## Frontend scope for this slice
- Add typed support for the new backend endpoints.
- Do not fake native push token acquisition in web runtime.
- Keep Android/iOS delivery readiness behind explicit device registration from a future mobile shell or wrapper.
