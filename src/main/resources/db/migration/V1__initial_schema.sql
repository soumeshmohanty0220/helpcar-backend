-- HelpCAR initial schema.
--
-- Geography vs geometry: every spatial column is `geography(_, 4326)` so PostGIS does
-- distance maths on the spheroid and ST_DWithin takes metres directly. Using `geometry`
-- with lat/lon would make "within 5000" mean 5000 degrees, which is the classic
-- silently-wrong PostGIS bug.

CREATE EXTENSION IF NOT EXISTS postgis;

-- Sets updated_at on every UPDATE so application code cannot forget to.
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- ---------------------------------------------------------------------------
-- Identity
-- ---------------------------------------------------------------------------

CREATE TABLE users (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name               varchar(120) NOT NULL,
    email              varchar(255) NOT NULL,
    phone              varchar(32)  NOT NULL,
    password_hash      varchar(255) NOT NULL,
    reliability_rating numeric(3, 2) NOT NULL DEFAULT 5.00,
    enabled            boolean NOT NULL DEFAULT true,
    version            integer NOT NULL DEFAULT 0,
    created_at         timestamptz NOT NULL DEFAULT now(),
    updated_at         timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_reliability CHECK (reliability_rating BETWEEN 0 AND 5)
);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Roles are additive: one account can be both a requester and a helper.
CREATE TABLE user_roles (
    user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role    varchar(32) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT chk_user_roles_role CHECK (role IN ('REQUESTER', 'HELPER', 'ADMIN'))
);


-- ---------------------------------------------------------------------------
-- Availability — routes helpers offer to drive
-- ---------------------------------------------------------------------------

CREATE TABLE helper_routes (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    helper_id         uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    origin            geography(Point, 4326) NOT NULL,
    destination       geography(Point, 4326) NOT NULL,
    route_line        geography(LineString, 4326) NOT NULL,
    origin_label      varchar(255),
    destination_label varchar(255),
    time_window       tstzrange NOT NULL,
    seats_total       smallint NOT NULL DEFAULT 1,
    seats_available   smallint NOT NULL DEFAULT 1,
    status            varchar(16) NOT NULL DEFAULT 'ACTIVE',
    version           integer NOT NULL DEFAULT 0,
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_helper_routes_status
        CHECK (status IN ('ACTIVE', 'MATCHED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_helper_routes_seats
        CHECK (seats_total > 0 AND seats_available >= 0 AND seats_available <= seats_total)
);

CREATE TRIGGER trg_helper_routes_updated_at
    BEFORE UPDATE ON helper_routes
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Stage 1 of the matching pipeline lives on this index: it turns "helper routes near
-- this pickup point" into an index seek instead of a scan over every helper.
CREATE INDEX idx_helper_routes_line_gist ON helper_routes USING GIST (route_line);
CREATE INDEX idx_helper_routes_origin_gist ON helper_routes USING GIST (origin);
CREATE INDEX idx_helper_routes_window_gist ON helper_routes USING GIST (time_window);
CREATE INDEX idx_helper_routes_helper ON helper_routes (helper_id);
-- Partial index: matching only ever queries ACTIVE routes, so the index stays small
-- even as completed rides accumulate.
CREATE INDEX idx_helper_routes_active ON helper_routes (status) WHERE status = 'ACTIVE';


-- ---------------------------------------------------------------------------
-- Ride requests
-- ---------------------------------------------------------------------------

CREATE TABLE ride_requests (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_id     uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    pickup           geography(Point, 4326) NOT NULL,
    dropoff          geography(Point, 4326) NOT NULL,
    pickup_label     varchar(255),
    dropoff_label    varchar(255),
    mode             varchar(16) NOT NULL DEFAULT 'MEDICAL',
    status           varchar(16) NOT NULL DEFAULT 'PENDING',
    requested_window tstzrange NOT NULL,
    notes            varchar(500),
    version          integer NOT NULL DEFAULT 0,
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_ride_requests_mode CHECK (mode IN ('MEDICAL', 'CARPOOL')),
    CONSTRAINT chk_ride_requests_status
        CHECK (status IN ('PENDING', 'MATCHED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'EXPIRED'))
);

CREATE TRIGGER trg_ride_requests_updated_at
    BEFORE UPDATE ON ride_requests
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX idx_ride_requests_pickup_gist ON ride_requests USING GIST (pickup);
CREATE INDEX idx_ride_requests_requester ON ride_requests (requester_id);
CREATE INDEX idx_ride_requests_pending ON ride_requests (status) WHERE status = 'PENDING';


-- ---------------------------------------------------------------------------
-- Matches
-- ---------------------------------------------------------------------------

CREATE TABLE matches (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    ride_request_id     uuid NOT NULL REFERENCES ride_requests (id) ON DELETE CASCADE,
    helper_route_id     uuid NOT NULL REFERENCES helper_routes (id) ON DELETE CASCADE,
    detour_meters       numeric(10, 2) NOT NULL,
    score               numeric(6, 4),
    status              varchar(16) NOT NULL DEFAULT 'OFFERED',
    cancellation_reason varchar(255),
    offered_at          timestamptz NOT NULL DEFAULT now(),
    responded_at        timestamptz,
    completed_at        timestamptz,
    version             integer NOT NULL DEFAULT 0,
    CONSTRAINT chk_matches_status
        CHECK (status IN ('OFFERED', 'ACCEPTED', 'DECLINED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_matches_detour CHECK (detour_meters >= 0)
);

-- A ride request may only have one live match at a time. Enforced in the database
-- rather than in service code, so a race between two accepts cannot double-book.
CREATE UNIQUE INDEX uq_matches_one_live_per_request
    ON matches (ride_request_id)
    WHERE status IN ('OFFERED', 'ACCEPTED', 'IN_PROGRESS');

CREATE INDEX idx_matches_helper_route ON matches (helper_route_id);
CREATE INDEX idx_matches_status ON matches (status);


-- ---------------------------------------------------------------------------
-- Live location trail
-- ---------------------------------------------------------------------------

CREATE TABLE location_pings (
    id          bigserial PRIMARY KEY,
    match_id    uuid NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
    user_id     uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    position    geography(Point, 4326) NOT NULL,
    accuracy_m  real,
    recorded_at timestamptz NOT NULL DEFAULT now()
);

-- "Latest position for this ride" is the hot query; the descending time component
-- lets Postgres answer it from the index alone.
CREATE INDEX idx_location_pings_match_time ON location_pings (match_id, recorded_at DESC);
CREATE INDEX idx_location_pings_position_gist ON location_pings USING GIST (position);


-- ---------------------------------------------------------------------------
-- Ratings — feed the reliability term in the stage 4 ranking score
-- ---------------------------------------------------------------------------

CREATE TABLE ratings (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id   uuid NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
    rater_id   uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    ratee_id   uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    score      smallint NOT NULL,
    comment    varchar(500),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_ratings_score CHECK (score BETWEEN 1 AND 5),
    CONSTRAINT chk_ratings_not_self CHECK (rater_id <> ratee_id),
    CONSTRAINT uq_ratings_match_rater UNIQUE (match_id, rater_id)
);

CREATE INDEX idx_ratings_ratee ON ratings (ratee_id);
