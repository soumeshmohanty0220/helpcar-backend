/**
 * Notification bounded context — reaching users who are not looking at the app.
 *
 * <p>Owns: push delivery through Firebase Cloud Messaging and the in-app event log.
 * FCM is the one piece of Firebase the architecture keeps; auth, the database and
 * realtime sync all move into this service.
 *
 * <p>Consumes domain events from the other modules rather than being called directly by
 * them, so that adding a notification never means editing matching logic.
 *
 * <p>Depends on: all modules, as an event consumer only.
 */
package com.helpcar.backend.notification;
