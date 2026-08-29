/**
 * Ride request bounded context — trips people ask for.
 *
 * <p>Owns: the {@code ride_requests} table and the request lifecycle
 * ({@code PENDING → MATCHED → IN_PROGRESS → COMPLETED}, with {@code CANCELLED} and
 * {@code EXPIRED} as terminal branches). Every transition is validated server-side; the
 * client cannot put a request into a state it did not earn.
 *
 * <p>Carries the {@code mode} discriminator ({@code MEDICAL} or {@code CARPOOL}) that lets
 * the matching module weight urgency differently for the two audiences the product serves.
 *
 * <p>Depends on: {@code identity}.
 */
package com.helpcar.backend.riderequest;
