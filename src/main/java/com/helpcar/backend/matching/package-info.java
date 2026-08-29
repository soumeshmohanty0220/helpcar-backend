/**
 * Matching bounded context — pairing ride requests with helper routes.
 *
 * <p>The heart of the service. Implements the four-stage pipeline from
 * {@code docs/ARCHITECTURE.md} §6:
 *
 * <ol>
 *   <li><b>Spatial filter</b> — {@code ST_DWithin} plus the KNN {@code <->} operator against
 *       the {@code route_line} GiST index. O(log n + k) instead of the O(n) client-side scan.
 *   <li><b>Corridor and direction check</b> — {@code ST_Buffer} containment and
 *       {@code ST_LineLocatePoint} ordering, so pickup precedes dropoff along the helper's
 *       actual direction of travel. Pure geometry, no external calls.
 *   <li><b>Detour costing</b> — real road-network distance from the routing engine, scored
 *       by cheapest insertion: {@code (origin→pickup→dropoff→destination) − (origin→destination)}.
 *   <li><b>Ranking</b> — weighted score over detour, wait time and helper reliability.
 * </ol>
 *
 * <p>Structured ports-and-adapters: the domain depends on a {@code RoutingPort} interface,
 * never on a concrete routing client, so the engine stays unit-testable and swappable.
 *
 * <p>Concurrency: accepting a match takes an optimistic lock on the helper route's
 * {@code version} column, so two requesters cannot book the same route.
 *
 * <p>Depends on: {@code availability}, {@code riderequest}, and an external routing engine.
 */
package com.helpcar.backend.matching;
