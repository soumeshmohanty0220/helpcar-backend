/**
 * Availability bounded context — the routes helpers offer to drive.
 *
 * <p>Owns: the {@code helper_routes} table. A route carries its origin, destination and
 * the road-network geometry between them as PostGIS {@code geography} values, plus the
 * time window the helper is willing to travel in and the seats still free.
 *
 * <p>The {@code route_line} GiST index defined in {@code V1__initial_schema.sql} is what
 * lets the matching module find candidates with an index seek rather than the full-table
 * scan the mobile app used to run on the device.
 *
 * <p>Depends on: {@code identity}.
 */
package com.helpcar.backend.availability;
