/**
 * Location bounded context — live position ingestion and fan-out.
 *
 * <p>Owns: the STOMP-over-WebSocket endpoints, Redis {@code GEOADD}/{@code GEOSEARCH} writes,
 * and the {@code location_pings} audit trail.
 *
 * <p>Clients ping on an interval-or-distance trigger (every few seconds, or after moving a
 * meaningful distance — whichever comes first) rather than on every GPS tick, which would
 * burn battery and bandwidth for no matching benefit.
 *
 * <p>Fan-out goes through Redis pub/sub rather than a local in-memory broker: a requester's
 * socket may be held by a different instance than the one receiving the helper's ping, so
 * publishing through Redis is what makes horizontal scaling work without sticky sessions.
 *
 * <p>Depends on: {@code matching} (to know which requester may see which helper's position —
 * live location is only ever visible to the counterparty of an active match).
 */
package com.helpcar.backend.location;
