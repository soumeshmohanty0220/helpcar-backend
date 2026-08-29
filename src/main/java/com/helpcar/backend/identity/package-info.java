/**
 * Identity bounded context — users, roles and credentials.
 *
 * <p>Owns: the {@code users} and {@code user_roles} tables, password hashing, and JWT
 * issuance/refresh. Roles are additive: one account may hold both {@code REQUESTER} and
 * {@code HELPER}, which is how the product actually behaves.
 *
 * <p>This module is the single registration path. The legacy mobile app had two competing
 * ones — a custom HTTP endpoint that never created a Firebase Auth user, and a login screen
 * that only checked Firebase Auth — so accounts created in the app could never sign in.
 *
 * <p>Depends on: nothing. Every other module depends on this one.
 */
package com.helpcar.backend.identity;
