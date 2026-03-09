--
-- PostgreSQL database dump
--

\restrict z9UbsbOkQ9gdeH3eSZqAaP4vKQDItMCy5S1tAo9zLVx7A7Maf9irNUTHcHh2CHJ

-- Dumped from database version 16.11 (Ubuntu 16.11-0ubuntu0.24.04.1)
-- Dumped by pg_dump version 16.11 (Ubuntu 16.11-0ubuntu0.24.04.1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

DROP DATABASE IF EXISTS lfkdb;
--
-- Name: lfkdb; Type: DATABASE; Schema: -; Owner: postgres
--

CREATE DATABASE lfkdb WITH TEMPLATE = template0 ENCODING = 'UTF8' LOCALE_PROVIDER = libc LOCALE = 'ru_RU.UTF-8';


ALTER DATABASE lfkdb OWNER TO postgres;

\unrestrict z9UbsbOkQ9gdeH3eSZqAaP4vKQDItMCy5S1tAo9zLVx7A7Maf9irNUTHcHh2CHJ
\connect lfkdb
\restrict z9UbsbOkQ9gdeH3eSZqAaP4vKQDItMCy5S1tAo9zLVx7A7Maf9irNUTHcHh2CHJ

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


--
-- Name: update_updated_at_column(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_updated_at_column() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_updated_at_column() OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: achievements; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.achievements (
    id integer NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    icon character varying(50),
    condition_type character varying(50),
    condition_value integer,
    experience_points integer DEFAULT 10,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT achievements_condition_type_check CHECK (((condition_type)::text = ANY ((ARRAY['total_sessions'::character varying, 'streak'::character varying, 'accuracy'::character varying, 'total_exercises'::character varying])::text[]))),
    CONSTRAINT achievements_condition_value_check CHECK ((condition_value > 0)),
    CONSTRAINT achievements_experience_points_check CHECK ((experience_points >= 0))
);


ALTER TABLE public.achievements OWNER TO postgres;

--
-- Name: achievements_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.achievements_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.achievements_id_seq OWNER TO postgres;

--
-- Name: achievements_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.achievements_id_seq OWNED BY public.achievements.id;


--
-- Name: daily_stats; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.daily_stats (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    user_id uuid,
    stat_date date NOT NULL,
    total_sessions integer DEFAULT 0,
    total_duration_seconds integer DEFAULT 0,
    total_exercises integer DEFAULT 0,
    calories_burned numeric(10,2) DEFAULT 0,
    streak_day integer DEFAULT 0,
    completed boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT daily_stats_calories_burned_check CHECK ((calories_burned >= (0)::numeric)),
    CONSTRAINT daily_stats_streak_day_check CHECK ((streak_day >= 0)),
    CONSTRAINT daily_stats_total_duration_seconds_check CHECK ((total_duration_seconds >= 0)),
    CONSTRAINT daily_stats_total_exercises_check CHECK ((total_exercises >= 0)),
    CONSTRAINT daily_stats_total_sessions_check CHECK ((total_sessions >= 0))
);


ALTER TABLE public.daily_stats OWNER TO postgres;

--
-- Name: exercise_categories; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.exercise_categories (
    id integer NOT NULL,
    name character varying(50) NOT NULL,
    description text,
    icon character varying(50),
    color character varying(20),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.exercise_categories OWNER TO postgres;

--
-- Name: exercise_categories_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.exercise_categories_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.exercise_categories_id_seq OWNER TO postgres;

--
-- Name: exercise_categories_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.exercise_categories_id_seq OWNED BY public.exercise_categories.id;


--
-- Name: exercise_sets; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.exercise_sets (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    session_id uuid,
    exercise_id character varying(50),
    started_at timestamp without time zone NOT NULL,
    completed_at timestamp without time zone,
    target_repetitions integer,
    actual_repetitions integer,
    target_duration_seconds integer,
    actual_duration_seconds integer,
    accuracy_score numeric(5,2),
    completion_status character varying(20) DEFAULT 'completed'::character varying,
    performance_data jsonb,
    notes text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT exercise_sets_accuracy_score_check CHECK (((accuracy_score >= (0)::numeric) AND (accuracy_score <= (100)::numeric))),
    CONSTRAINT exercise_sets_actual_duration_seconds_check CHECK ((actual_duration_seconds >= 0)),
    CONSTRAINT exercise_sets_actual_repetitions_check CHECK ((actual_repetitions >= 0)),
    CONSTRAINT exercise_sets_completion_status_check CHECK (((completion_status)::text = ANY ((ARRAY['completed'::character varying, 'failed'::character varying, 'partial'::character varying])::text[]))),
    CONSTRAINT exercise_sets_target_duration_seconds_check CHECK ((target_duration_seconds >= 0)),
    CONSTRAINT exercise_sets_target_repetitions_check CHECK ((target_repetitions >= 0))
);


ALTER TABLE public.exercise_sets OWNER TO postgres;

--
-- Name: exercise_stats; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.exercise_stats (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    user_id uuid,
    exercise_id character varying(50),
    total_sessions integer DEFAULT 0,
    total_repetitions integer DEFAULT 0,
    total_duration integer DEFAULT 0,
    best_accuracy numeric(5,2),
    avg_accuracy numeric(5,2),
    last_performed_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.exercise_stats OWNER TO postgres;

--
-- Name: exercises; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.exercises (
    id character varying(50) NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    category_id integer,
    difficulty_level integer,
    target_muscles text[],
    instructions text[],
    duration_seconds integer,
    calories_burn numeric(5,2),
    video_url character varying(255),
    image_url character varying(255),
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    metadata jsonb,
    CONSTRAINT exercises_calories_burn_check CHECK ((calories_burn >= (0)::numeric)),
    CONSTRAINT exercises_difficulty_level_check CHECK (((difficulty_level >= 1) AND (difficulty_level <= 5))),
    CONSTRAINT exercises_duration_seconds_check CHECK ((duration_seconds > 0))
);


ALTER TABLE public.exercises OWNER TO postgres;

--
-- Name: overall_stats; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.overall_stats (
    user_id uuid NOT NULL,
    total_sessions integer DEFAULT 0,
    total_exercises integer DEFAULT 0,
    total_repetitions integer DEFAULT 0,
    total_duration integer DEFAULT 0,
    unique_exercises integer DEFAULT 0,
    current_streak integer DEFAULT 0,
    longest_streak integer DEFAULT 0,
    joined_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    last_workout_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.overall_stats OWNER TO postgres;

--
-- Name: user_achievements; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_achievements (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    user_id uuid,
    achievement_id integer,
    earned_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.user_achievements OWNER TO postgres;

--
-- Name: user_exercise_progress; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_exercise_progress (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    user_id uuid,
    exercise_id character varying(50),
    total_sessions integer DEFAULT 0,
    total_repetitions integer DEFAULT 0,
    total_duration_seconds integer DEFAULT 0,
    best_accuracy numeric(5,2),
    average_accuracy numeric(5,2),
    last_performed_at timestamp without time zone,
    current_streak integer DEFAULT 0,
    max_streak integer DEFAULT 0,
    level integer DEFAULT 1,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT user_exercise_progress_average_accuracy_check CHECK (((average_accuracy >= (0)::numeric) AND (average_accuracy <= (100)::numeric))),
    CONSTRAINT user_exercise_progress_best_accuracy_check CHECK (((best_accuracy >= (0)::numeric) AND (best_accuracy <= (100)::numeric))),
    CONSTRAINT user_exercise_progress_current_streak_check CHECK ((current_streak >= 0)),
    CONSTRAINT user_exercise_progress_level_check CHECK ((level > 0)),
    CONSTRAINT user_exercise_progress_max_streak_check CHECK ((max_streak >= 0)),
    CONSTRAINT user_exercise_progress_total_duration_seconds_check CHECK ((total_duration_seconds >= 0)),
    CONSTRAINT user_exercise_progress_total_repetitions_check CHECK ((total_repetitions >= 0)),
    CONSTRAINT user_exercise_progress_total_sessions_check CHECK ((total_sessions >= 0))
);


ALTER TABLE public.user_exercise_progress OWNER TO postgres;

--
-- Name: user_settings; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_settings (
    user_id uuid NOT NULL,
    notification_enabled boolean DEFAULT true,
    reminder_time time without time zone DEFAULT '09:00:00'::time without time zone,
    daily_goal_minutes integer DEFAULT 30,
    sound_enabled boolean DEFAULT true,
    voice_guide_enabled boolean DEFAULT true,
    theme character varying(20) DEFAULT 'light'::character varying,
    language character varying(10) DEFAULT 'ru'::character varying,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT user_settings_daily_goal_minutes_check CHECK ((daily_goal_minutes >= 0)),
    CONSTRAINT user_settings_language_check CHECK (((language)::text = ANY ((ARRAY['ru'::character varying, 'en'::character varying, 'uk'::character varying])::text[]))),
    CONSTRAINT user_settings_theme_check CHECK (((theme)::text = ANY ((ARRAY['light'::character varying, 'dark'::character varying, 'system'::character varying])::text[])))
);


ALTER TABLE public.user_settings OWNER TO postgres;

--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    username character varying(50) NOT NULL,
    email character varying(100) NOT NULL,
    password_hash character varying(255) NOT NULL,
    first_name character varying(50),
    last_name character varying(50),
    birth_date date,
    gender character varying(10),
    height_cm integer,
    weight_kg numeric(5,2),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    last_login timestamp without time zone,
    is_active boolean DEFAULT true,
    role character varying(20) DEFAULT 'user'::character varying,
    CONSTRAINT users_gender_check CHECK (((gender)::text = ANY ((ARRAY['male'::character varying, 'female'::character varying, 'other'::character varying])::text[]))),
    CONSTRAINT users_height_cm_check CHECK (((height_cm > 0) AND (height_cm < 300))),
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['user'::character varying, 'admin'::character varying, 'trainer'::character varying])::text[]))),
    CONSTRAINT users_weight_kg_check CHECK (((weight_kg > (0)::numeric) AND (weight_kg < (500)::numeric)))
);


ALTER TABLE public.users OWNER TO postgres;

--
-- Name: workout_sessions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.workout_sessions (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    user_id uuid,
    started_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    ended_at timestamp without time zone,
    duration_seconds integer,
    status character varying(20) DEFAULT 'in_progress'::character varying,
    notes text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT workout_sessions_duration_seconds_check CHECK ((duration_seconds >= 0)),
    CONSTRAINT workout_sessions_status_check CHECK (((status)::text = ANY ((ARRAY['in_progress'::character varying, 'completed'::character varying, 'abandoned'::character varying])::text[])))
);


ALTER TABLE public.workout_sessions OWNER TO postgres;

--
-- Name: achievements id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.achievements ALTER COLUMN id SET DEFAULT nextval('public.achievements_id_seq'::regclass);


--
-- Name: exercise_categories id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.exercise_categories ALTER COLUMN id SET DEFAULT nextval('public.exercise_categories_id_seq'::regclass);


--
-- Data for Name: achievements; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: daily_stats; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.daily_stats VALUES ('7c6bd310-cc9d-48b3-a2d5-ad9ae18e08dd', '643b6d41-9df6-4e48-ac9b-12eb5e714b85', '2026-03-03', 1, 60, 1, 3.00, 0, false, '2026-03-03 20:40:17.09251', '2026-03-03 20:40:17.09251');
INSERT INTO public.daily_stats VALUES ('86835e6f-81ec-46b7-a039-13d7e080d054', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03', 5, 300, 5, 15.00, 0, false, '2026-03-03 18:30:02.232912', '2026-03-03 21:58:53.971978');
INSERT INTO public.daily_stats VALUES ('067077b3-0411-4276-9ece-f7aa6b0a6bed', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-04', 2, 120, 2, 6.00, 0, false, '2026-03-04 17:41:22.927789', '2026-03-04 18:23:46.786804');
INSERT INTO public.daily_stats VALUES ('fd291634-d124-4860-81a8-fcef6e1ede62', 'adaf71fa-100e-4ad6-af29-e582414908a2', '2026-02-20', 70, 4200, 70, 210.00, 0, false, '2026-02-20 16:36:34.649751', '2026-02-20 22:46:24.317925');
INSERT INTO public.daily_stats VALUES ('e4ee3608-17c2-4177-91d2-69ffff01bed2', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-20', 1, 60, 1, 3.00, 0, false, '2026-02-20 23:31:35.585538', '2026-02-20 23:31:35.585538');
INSERT INTO public.daily_stats VALUES ('bd50f284-9148-4364-b2a4-1ec8531cfbd0', '643b6d41-9df6-4e48-ac9b-12eb5e714b85', '2026-02-26', 3, 115, 3, 5.75, 0, false, '2026-02-26 14:45:22.662518', '2026-02-26 14:45:23.700102');
INSERT INTO public.daily_stats VALUES ('c7477a96-5287-4561-8d74-d93e33423ee0', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-01', 10, 600, 10, 30.00, 0, false, '2026-03-01 17:56:39.704428', '2026-03-01 17:58:17.77111');
INSERT INTO public.daily_stats VALUES ('d2bf169a-0b79-41a2-8795-39984a4e99e5', 'ea47d1d7-c7a0-4e6a-b0fa-fa0551047349', '2026-03-01', 18, 1080, 18, 54.00, 0, false, '2026-03-01 18:49:44.467276', '2026-03-01 19:05:06.337111');
INSERT INTO public.daily_stats VALUES ('d455a2d5-a96f-416b-a421-308f590ecc17', 'b4fd3333-1d59-421e-bb18-198df724a66d', '2026-03-01', 5, 300, 5, 15.00, 0, false, '2026-03-01 19:08:12.836344', '2026-03-01 19:08:37.455727');


--
-- Data for Name: exercise_categories; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.exercise_categories VALUES (1, 'Руки', 'Упражнения для кистей и пальцев рук', '👐', '#FF6B6B', '2026-02-14 00:11:01.354353');
INSERT INTO public.exercise_categories VALUES (2, 'Плечи', 'Упражнения для плечевого пояса', '💪', '#4ECDC4', '2026-02-14 00:11:01.354353');
INSERT INTO public.exercise_categories VALUES (3, 'Спина', 'Упражнения для спины', '⬆️', '#45B7D1', '2026-02-14 00:11:01.354353');
INSERT INTO public.exercise_categories VALUES (4, 'Моторика', 'Упражнения для развития мелкой моторики пальцев', '🤏', '#9B59B6', '2026-03-04 15:11:07.780614');


--
-- Data for Name: exercise_sets; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.exercise_sets VALUES ('dc9ceb00-c1c2-42ad-a316-aa6a06a963ee', 'a2b9b130-ae5f-4987-bf47-64a08252d265', 'fist', '2026-02-20 15:27:14.116335', '2026-02-20 15:27:44.116335', NULL, 10, NULL, 30, 94.50, 'completed', NULL, NULL, '2026-02-20 15:27:14.116707');
INSERT INTO public.exercise_sets VALUES ('b13eb381-1296-4ed9-8e32-1e1be19af7c7', 'a2b9b130-ae5f-4987-bf47-64a08252d265', 'fist-index', '2026-02-20 15:27:14.628111', '2026-02-20 15:27:39.628111', NULL, 8, NULL, 25, 87.00, 'completed', NULL, NULL, '2026-02-20 15:27:14.628447');
INSERT INTO public.exercise_sets VALUES ('b08153b2-9aa3-4a41-9d7e-bfb289c93e9d', 'a2b9b130-ae5f-4987-bf47-64a08252d265', 'fist-palm', '2026-02-20 15:27:15.138835', '2026-02-20 15:28:15.138835', NULL, 5, NULL, 60, 91.30, 'completed', NULL, NULL, '2026-02-20 15:27:15.138986');
INSERT INTO public.exercise_sets VALUES ('54aba109-b4cf-4abd-b084-d3dc95b56bcc', 'fa2550bd-90c9-45d0-9102-d2d1784c4101', 'fist', '2026-02-20 15:27:15.655121', '2026-02-20 15:28:15.655121', NULL, 20, NULL, 60, 93.50, 'completed', NULL, NULL, '2026-02-20 15:27:15.655307');
INSERT INTO public.exercise_sets VALUES ('72c0d06a-3d5e-452c-bb9a-6b97a22c0350', 'fa2550bd-90c9-45d0-9102-d2d1784c4101', 'fist-index', '2026-02-20 15:27:16.165843', '2026-02-20 15:28:06.165843', NULL, 16, NULL, 50, 86.00, 'completed', NULL, NULL, '2026-02-20 15:27:16.1661');
INSERT INTO public.exercise_sets VALUES ('0eddcb0b-ab6c-4cd4-bfb8-586891effd4b', 'fa2550bd-90c9-45d0-9102-d2d1784c4101', 'fist-palm', '2026-02-20 15:27:16.676541', '2026-02-20 15:29:16.676541', NULL, 10, NULL, 120, 90.30, 'completed', NULL, NULL, '2026-02-20 15:27:16.676796');
INSERT INTO public.exercise_sets VALUES ('3e08238d-2fe9-48c2-9e5c-3651205b0af0', '53c6c402-f95f-4abb-ac9b-8c674ba3bf5f', 'fist', '2026-02-20 15:27:17.190283', '2026-02-20 15:28:47.190283', NULL, 30, NULL, 90, 92.50, 'completed', NULL, NULL, '2026-02-20 15:27:17.190488');
INSERT INTO public.exercise_sets VALUES ('893c9cef-4dba-432a-802c-c51b08199b23', '53c6c402-f95f-4abb-ac9b-8c674ba3bf5f', 'fist-index', '2026-02-20 15:27:17.70081', '2026-02-20 15:28:32.70081', NULL, 24, NULL, 75, 85.00, 'completed', NULL, NULL, '2026-02-20 15:27:17.701091');
INSERT INTO public.exercise_sets VALUES ('79efbf35-f25c-4b83-b62d-91de03017c87', '53c6c402-f95f-4abb-ac9b-8c674ba3bf5f', 'fist-palm', '2026-02-20 15:27:18.211538', '2026-02-20 15:30:18.211538', NULL, 15, NULL, 180, 89.30, 'completed', NULL, NULL, '2026-02-20 15:27:18.211802');
INSERT INTO public.exercise_sets VALUES ('33c86653-5b15-4ed7-8792-a52058d2dfd2', '2d67fc44-e5b5-46b5-9e03-15a5ad273e3f', 'fist', '2026-02-20 16:09:23.097497', '2026-02-20 16:09:53.097497', NULL, 10, NULL, 30, 94.50, 'completed', NULL, NULL, '2026-02-20 16:09:23.105909');
INSERT INTO public.exercise_sets VALUES ('9efb9f9a-0b26-4b14-937f-28144ff46105', '2d67fc44-e5b5-46b5-9e03-15a5ad273e3f', 'fist-index', '2026-02-20 16:09:23.610007', '2026-02-20 16:09:48.610007', NULL, 8, NULL, 25, 87.00, 'completed', NULL, NULL, '2026-02-20 16:09:23.610227');
INSERT INTO public.exercise_sets VALUES ('9666055c-f10c-49b9-908e-f5b5732e2197', '2d67fc44-e5b5-46b5-9e03-15a5ad273e3f', 'fist-palm', '2026-02-20 16:09:24.120666', '2026-02-20 16:10:24.120666', NULL, 5, NULL, 60, 91.30, 'completed', NULL, NULL, '2026-02-20 16:09:24.120896');
INSERT INTO public.exercise_sets VALUES ('e5f38a3a-c94c-4468-93e9-14ce42b45dad', '0cb81407-962d-4e48-9fc2-ab5bcf0349a5', 'fist', '2026-02-20 16:09:24.636637', '2026-02-20 16:10:24.636637', NULL, 20, NULL, 60, 93.50, 'completed', NULL, NULL, '2026-02-20 16:09:24.636855');
INSERT INTO public.exercise_sets VALUES ('4b2efee8-536d-4069-96a7-d382ddf01548', '0cb81407-962d-4e48-9fc2-ab5bcf0349a5', 'fist-index', '2026-02-20 16:09:25.147071', '2026-02-20 16:10:15.147071', NULL, 16, NULL, 50, 86.00, 'completed', NULL, NULL, '2026-02-20 16:09:25.147344');
INSERT INTO public.exercise_sets VALUES ('a1e83e22-1287-4f1c-95e2-27f900b5c383', '0cb81407-962d-4e48-9fc2-ab5bcf0349a5', 'fist-palm', '2026-02-20 16:09:25.657646', '2026-02-20 16:11:25.657646', NULL, 10, NULL, 120, 90.30, 'completed', NULL, NULL, '2026-02-20 16:09:25.657896');
INSERT INTO public.exercise_sets VALUES ('a688b2f7-ac41-424f-b108-5fb2ecadf44f', '3ff9aacf-1b2a-4c72-8c91-993f95354cfb', 'fist', '2026-02-20 16:09:26.172043', '2026-02-20 16:10:56.172043', NULL, 30, NULL, 90, 92.50, 'completed', NULL, NULL, '2026-02-20 16:09:26.172241');
INSERT INTO public.exercise_sets VALUES ('cc233589-9b24-49cd-b54b-de70c945224f', '3ff9aacf-1b2a-4c72-8c91-993f95354cfb', 'fist-index', '2026-02-20 16:09:26.682593', '2026-02-20 16:10:41.682593', NULL, 24, NULL, 75, 85.00, 'completed', NULL, NULL, '2026-02-20 16:09:26.68294');
INSERT INTO public.exercise_sets VALUES ('334fe1b9-0787-4efb-bd99-5adb06bae9f7', '3ff9aacf-1b2a-4c72-8c91-993f95354cfb', 'fist-palm', '2026-02-20 16:09:27.193263', '2026-02-20 16:12:27.193263', NULL, 15, NULL, 180, 89.30, 'completed', NULL, NULL, '2026-02-20 16:09:27.193513');
INSERT INTO public.exercise_sets VALUES ('beb84ce8-59ef-4dda-a4d4-99a3f57a3a66', '2d40a994-0840-457e-b137-5a5acebc5591', 'fist-palm', '2026-02-20 16:36:34.639355', '2026-02-20 16:37:34.639355', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:36:34.639666');
INSERT INTO public.exercise_sets VALUES ('f1758b09-6d41-4f6b-891d-44714750992c', '7e43b46f-fffc-4ac2-b393-863576ca914e', 'fist-palm', '2026-02-20 16:37:14.246626', '2026-02-20 16:38:14.246626', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:37:14.246917');
INSERT INTO public.exercise_sets VALUES ('c8ab9819-4660-46d0-b6ae-39747314543a', '7bb695c6-9398-4cc0-9473-a42cc57b4bab', 'fist-palm', '2026-02-20 16:42:20.285', '2026-02-20 16:43:20.285', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:42:20.285193');
INSERT INTO public.exercise_sets VALUES ('8c461c74-6197-4ba2-9921-e578d216fe53', '7bb695c6-9398-4cc0-9473-a42cc57b4bab', 'fist-palm', '2026-02-20 16:42:20.825397', '2026-02-20 16:43:20.825397', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:42:20.825699');
INSERT INTO public.exercise_sets VALUES ('b16ce644-6ec1-44f6-b30a-4f72e0bb8487', '7bb695c6-9398-4cc0-9473-a42cc57b4bab', 'fist-palm', '2026-02-20 16:42:21.36507', '2026-02-20 16:43:21.36507', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:42:21.365251');
INSERT INTO public.exercise_sets VALUES ('3ffe85ff-7790-4c95-bd4f-bfd8706430b6', '7bb695c6-9398-4cc0-9473-a42cc57b4bab', 'fist-palm', '2026-02-20 16:42:21.904561', '2026-02-20 16:43:21.904561', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:42:21.904832');
INSERT INTO public.exercise_sets VALUES ('33947e9e-c16e-4ba3-9c08-64bed75b2bbc', '7bb695c6-9398-4cc0-9473-a42cc57b4bab', 'fist-palm', '2026-02-20 16:42:22.445375', '2026-02-20 16:43:22.445375', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:42:22.445579');
INSERT INTO public.exercise_sets VALUES ('a7e29515-b873-4d96-9830-77b0b0076ef8', 'cbee99c3-f939-4940-b0be-b0a205b4b68f', 'fist-palm', '2026-02-20 16:42:27.765877', '2026-02-20 16:43:27.765877', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:42:27.766117');
INSERT INTO public.exercise_sets VALUES ('86da9340-3984-4ee1-a965-cda6f95a70bc', 'cbee99c3-f939-4940-b0be-b0a205b4b68f', 'fist-palm', '2026-02-20 16:42:28.305446', '2026-02-20 16:43:28.305446', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:42:28.30562');
INSERT INTO public.exercise_sets VALUES ('92758d21-ea96-475e-b341-f9064eab590e', 'cbee99c3-f939-4940-b0be-b0a205b4b68f', 'fist-palm', '2026-02-20 16:42:28.845814', '2026-02-20 16:43:28.845814', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:42:28.846047');
INSERT INTO public.exercise_sets VALUES ('a667233a-92c3-44c5-90b6-d65db878f712', 'a4e89c7c-1125-4dd7-b3ae-55b23e02adde', 'fist-palm', '2026-02-20 16:43:04.957788', '2026-02-20 16:44:04.957788', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:43:04.958011');
INSERT INTO public.exercise_sets VALUES ('8270ddf5-03f3-45f4-ba83-7ba97431326b', 'a4e89c7c-1125-4dd7-b3ae-55b23e02adde', 'fist-palm', '2026-02-20 16:43:05.498883', '2026-02-20 16:44:05.498883', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:43:05.499056');
INSERT INTO public.exercise_sets VALUES ('57f705a7-3652-42e7-b56f-6cd46646ad93', 'a4e89c7c-1125-4dd7-b3ae-55b23e02adde', 'fist-palm', '2026-02-20 16:43:06.037743', '2026-02-20 16:44:06.037743', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:43:06.038022');
INSERT INTO public.exercise_sets VALUES ('c3de84c7-c328-447c-8443-57586cae6cfd', 'a4e89c7c-1125-4dd7-b3ae-55b23e02adde', 'fist-palm', '2026-02-20 16:43:06.578326', '2026-02-20 16:44:06.578326', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:43:06.578508');
INSERT INTO public.exercise_sets VALUES ('fefbc064-b28b-496b-b3a8-7412e0924167', 'a4e89c7c-1125-4dd7-b3ae-55b23e02adde', 'fist-palm', '2026-02-20 16:43:07.118593', '2026-02-20 16:44:07.118593', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:43:07.118841');
INSERT INTO public.exercise_sets VALUES ('8c34c9c0-b706-4dba-8aef-e88122e61ae0', 'a4e89c7c-1125-4dd7-b3ae-55b23e02adde', 'fist-palm', '2026-02-20 16:43:07.658211', '2026-02-20 16:44:07.658211', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:43:07.658383');
INSERT INTO public.exercise_sets VALUES ('2e563c99-54d2-4c39-a506-a28e3f076e40', 'a4e89c7c-1125-4dd7-b3ae-55b23e02adde', 'fist-palm', '2026-02-20 16:43:08.198165', '2026-02-20 16:44:08.198165', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:43:08.198341');
INSERT INTO public.exercise_sets VALUES ('0f99b0c7-ef8f-4e45-92cc-0fff1fb672a8', 'a4e89c7c-1125-4dd7-b3ae-55b23e02adde', 'fist-palm', '2026-02-20 16:43:08.738503', '2026-02-20 16:44:08.738503', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:43:08.738715');
INSERT INTO public.exercise_sets VALUES ('954482b0-b72c-485c-806e-ab05f79796fa', 'a4e89c7c-1125-4dd7-b3ae-55b23e02adde', 'fist-palm', '2026-02-20 16:43:09.278804', '2026-02-20 16:44:09.278804', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:43:09.278997');
INSERT INTO public.exercise_sets VALUES ('0e1edc8a-075c-4928-b0f7-3bd41c3f2e7c', 'a4e89c7c-1125-4dd7-b3ae-55b23e02adde', 'fist-palm', '2026-02-20 16:43:09.821942', '2026-02-20 16:44:09.821942', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:43:09.822163');
INSERT INTO public.exercise_sets VALUES ('f34ca245-e03f-4584-ac13-3537e3996076', 'a4e89c7c-1125-4dd7-b3ae-55b23e02adde', 'fist-palm', '2026-02-20 16:43:10.359819', '2026-02-20 16:44:10.359819', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:43:10.360055');
INSERT INTO public.exercise_sets VALUES ('a16e249c-870d-400b-a624-62bd533d7fa5', 'a4e89c7c-1125-4dd7-b3ae-55b23e02adde', 'fist-palm', '2026-02-20 16:43:10.898313', '2026-02-20 16:44:10.898313', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:43:10.898526');
INSERT INTO public.exercise_sets VALUES ('34176ac0-fa34-49f8-bcbc-9f00f0fd0a8e', 'a4e89c7c-1125-4dd7-b3ae-55b23e02adde', 'fist-palm', '2026-02-20 16:43:11.438276', '2026-02-20 16:44:11.438276', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:43:11.438564');
INSERT INTO public.exercise_sets VALUES ('f0f934bc-cc6d-4a51-bb17-09bc235893e7', 'a4e89c7c-1125-4dd7-b3ae-55b23e02adde', 'fist-palm', '2026-02-20 16:43:11.978026', '2026-02-20 16:44:11.978026', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:43:11.978208');
INSERT INTO public.exercise_sets VALUES ('e2b1ece5-9d8f-4f43-b8c8-841ead7dc89b', 'a4e89c7c-1125-4dd7-b3ae-55b23e02adde', 'fist-palm', '2026-02-20 16:43:12.51848', '2026-02-20 16:44:12.51848', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:43:12.518743');
INSERT INTO public.exercise_sets VALUES ('b5fe5aae-1535-43c8-b2eb-fffc764b137f', 'a4e89c7c-1125-4dd7-b3ae-55b23e02adde', 'fist-palm', '2026-02-20 16:43:13.058534', '2026-02-20 16:44:13.058534', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:43:13.058857');
INSERT INTO public.exercise_sets VALUES ('e2c5be36-90bc-40e6-880c-82dc0ef47aee', 'a4e89c7c-1125-4dd7-b3ae-55b23e02adde', 'fist-palm', '2026-02-20 16:43:13.598406', '2026-02-20 16:44:13.598406', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:43:13.598603');
INSERT INTO public.exercise_sets VALUES ('3516a8b1-e8a9-48c7-aa67-796838fdb8f8', 'a4e89c7c-1125-4dd7-b3ae-55b23e02adde', 'fist-palm', '2026-02-20 16:43:14.138346', '2026-02-20 16:44:14.138346', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:43:14.138509');
INSERT INTO public.exercise_sets VALUES ('9110d6ba-5081-4bdd-a5da-8c7d5d8b4188', '3f775be0-98c8-4c98-8635-b188066f15a3', 'fist-palm', '2026-02-20 16:50:19.398439', '2026-02-20 16:51:19.398439', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:50:19.398652');
INSERT INTO public.exercise_sets VALUES ('cf2aac58-e2ff-4e3a-ac3d-3c07488b5fa0', '3f775be0-98c8-4c98-8635-b188066f15a3', 'fist-palm', '2026-02-20 16:50:19.93995', '2026-02-20 16:51:19.93995', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:50:19.940123');
INSERT INTO public.exercise_sets VALUES ('797754c5-c0d7-442b-acbf-5603a2b5af5e', '3f775be0-98c8-4c98-8635-b188066f15a3', 'fist-palm', '2026-02-20 16:50:20.479116', '2026-02-20 16:51:20.479116', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:50:20.479287');
INSERT INTO public.exercise_sets VALUES ('b558de2b-7e24-4a96-8f09-bbd8a77f7eaf', '3f775be0-98c8-4c98-8635-b188066f15a3', 'fist-palm', '2026-02-20 16:50:21.018865', '2026-02-20 16:51:21.018865', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:50:21.019097');
INSERT INTO public.exercise_sets VALUES ('a8ac2bb3-472c-4a80-b6c0-18247fd97580', '3f775be0-98c8-4c98-8635-b188066f15a3', 'fist-palm', '2026-02-20 16:50:21.55847', '2026-02-20 16:51:21.55847', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:50:21.558658');
INSERT INTO public.exercise_sets VALUES ('2573add3-2a1b-4dc9-a673-9beb1b24f30b', '3f775be0-98c8-4c98-8635-b188066f15a3', 'fist-palm', '2026-02-20 16:50:22.09844', '2026-02-20 16:51:22.09844', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:50:22.098728');
INSERT INTO public.exercise_sets VALUES ('76baad1f-3c43-455d-93cb-4d963b00991e', '64fa6ac8-a9b2-4d1c-8770-1c7b16f4deb5', 'fist-palm', '2026-02-20 16:50:32.639468', '2026-02-20 16:51:32.639468', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:50:32.63982');
INSERT INTO public.exercise_sets VALUES ('d44dca75-9221-4cb4-922d-2a8b6ed6e72b', '64fa6ac8-a9b2-4d1c-8770-1c7b16f4deb5', 'fist-palm', '2026-02-20 16:50:33.179683', '2026-02-20 16:51:33.179683', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:50:33.179915');
INSERT INTO public.exercise_sets VALUES ('5ab2ddfe-1809-46fb-a7cc-607f97874f28', '64fa6ac8-a9b2-4d1c-8770-1c7b16f4deb5', 'fist-palm', '2026-02-20 16:50:33.719491', '2026-02-20 16:51:33.719491', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:50:33.719727');
INSERT INTO public.exercise_sets VALUES ('08a366d6-00e0-42c3-bd71-07ef83cfd007', '64fa6ac8-a9b2-4d1c-8770-1c7b16f4deb5', 'fist-palm', '2026-02-20 16:50:34.259149', '2026-02-20 16:51:34.259149', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:50:34.259381');
INSERT INTO public.exercise_sets VALUES ('1068aa02-e307-4042-9b0b-45255a2287ec', '8bb68f6a-816b-421e-b62a-b956ef926542', 'fist-palm', '2026-02-20 16:55:36.062358', '2026-02-20 16:56:36.062358', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:55:36.06254');
INSERT INTO public.exercise_sets VALUES ('dde93e69-7974-402f-a8cd-cdb75f78d1de', '8bb68f6a-816b-421e-b62a-b956ef926542', 'fist-palm', '2026-02-20 16:55:36.603352', '2026-02-20 16:56:36.603352', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:55:36.603702');
INSERT INTO public.exercise_sets VALUES ('b31143c1-0632-4c90-bb15-c1657dedb088', '8bb68f6a-816b-421e-b62a-b956ef926542', 'fist-palm', '2026-02-20 16:55:37.142205', '2026-02-20 16:56:37.142205', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:55:37.142404');
INSERT INTO public.exercise_sets VALUES ('7b66c8a7-c728-40e5-b693-4a4e88e1bf9f', '8bb68f6a-816b-421e-b62a-b956ef926542', 'fist-palm', '2026-02-20 16:55:37.681917', '2026-02-20 16:56:37.681917', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:55:37.682156');
INSERT INTO public.exercise_sets VALUES ('ca734b47-9c81-463d-973b-3124734f3b2d', '8bb68f6a-816b-421e-b62a-b956ef926542', 'fist-palm', '2026-02-20 16:55:38.223854', '2026-02-20 16:56:38.223854', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:55:38.224084');
INSERT INTO public.exercise_sets VALUES ('47490226-164e-4018-a804-7d1337e440f9', 'd98699a0-0264-4e8e-818b-11bbb2004f0d', 'fist-palm', '2026-02-20 16:55:48.960462', '2026-02-20 16:56:48.960462', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:55:48.96064');
INSERT INTO public.exercise_sets VALUES ('cb15d1b8-17a6-4def-b2c2-cf9e9cb39dbe', 'd98699a0-0264-4e8e-818b-11bbb2004f0d', 'fist-palm', '2026-02-20 16:55:49.500968', '2026-02-20 16:56:49.500968', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:55:49.501192');
INSERT INTO public.exercise_sets VALUES ('a885b6b7-d165-47b4-9976-43e24b2fb3b5', '547c2aff-a5dd-4ee3-9462-b62f51b2cd0d', 'fist-palm', '2026-02-20 16:57:48.539719', '2026-02-20 16:58:48.539719', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:57:48.540026');
INSERT INTO public.exercise_sets VALUES ('ff87af8a-e129-4ac7-9f11-bc4476a8bf85', '547c2aff-a5dd-4ee3-9462-b62f51b2cd0d', 'fist-palm', '2026-02-20 16:57:49.139447', '2026-02-20 16:58:49.139447', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:57:49.139779');
INSERT INTO public.exercise_sets VALUES ('4f65a45f-7e3f-46e8-9426-c9ac2d6c2dc7', 'f52ba3d9-f1c4-40cd-8cf3-ab8074d2e84d', 'fist-palm', '2026-02-20 16:57:56.925379', '2026-02-20 16:58:56.925379', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:57:56.92557');
INSERT INTO public.exercise_sets VALUES ('30c83c18-0a00-48e8-a158-356b6c3cce8a', 'f52ba3d9-f1c4-40cd-8cf3-ab8074d2e84d', 'fist-palm', '2026-02-20 16:57:57.524236', '2026-02-20 16:58:57.524236', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:57:57.524462');
INSERT INTO public.exercise_sets VALUES ('0b8b83fc-f058-403b-bcbb-822f88f436bd', 'f52ba3d9-f1c4-40cd-8cf3-ab8074d2e84d', 'fist-palm', '2026-02-20 16:57:58.125085', '2026-02-20 16:58:58.125085', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:57:58.125307');
INSERT INTO public.exercise_sets VALUES ('e6edb373-7d1f-40b3-9d46-37661574462e', 'f52ba3d9-f1c4-40cd-8cf3-ab8074d2e84d', 'fist-palm', '2026-02-20 16:57:58.725942', '2026-02-20 16:58:58.725942', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:57:58.726122');
INSERT INTO public.exercise_sets VALUES ('e097ecfb-bf55-49fa-9c0a-0b5581f99152', 'f52ba3d9-f1c4-40cd-8cf3-ab8074d2e84d', 'fist-palm', '2026-02-20 16:57:59.324538', '2026-02-20 16:58:59.324538', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 16:57:59.324752');
INSERT INTO public.exercise_sets VALUES ('426e67d5-eea7-4d84-ac4f-11b7432ce6fb', 'd3ecac06-a77b-40f8-9b05-06e3590871ff', 'fist-palm', '2026-02-20 17:00:04.885417', '2026-02-20 17:01:04.885417', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 17:00:04.885596');
INSERT INTO public.exercise_sets VALUES ('8c19b348-9bf3-4bb2-9933-48e9f66b959f', 'd3ecac06-a77b-40f8-9b05-06e3590871ff', 'fist-palm', '2026-02-20 17:00:05.484885', '2026-02-20 17:01:05.484885', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 17:00:05.485197');
INSERT INTO public.exercise_sets VALUES ('c5260d6f-e9e9-4f1c-9fe4-d14567744f0b', '339bc908-ab71-40c3-8b20-6e7c7f29b496', 'fist-palm', '2026-02-20 17:00:10.933421', '2026-02-20 17:01:10.933421', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 17:00:10.933605');
INSERT INTO public.exercise_sets VALUES ('36948d65-f747-469e-adff-c924533c0373', '339bc908-ab71-40c3-8b20-6e7c7f29b496', 'fist-palm', '2026-02-20 17:00:11.525963', '2026-02-20 17:01:11.525963', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 17:00:11.526158');
INSERT INTO public.exercise_sets VALUES ('4acd5789-fd03-49ea-a582-38d384b68f89', '339bc908-ab71-40c3-8b20-6e7c7f29b496', 'fist-palm', '2026-02-20 17:00:12.124074', '2026-02-20 17:01:12.124074', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 17:00:12.124423');
INSERT INTO public.exercise_sets VALUES ('1e87e7e1-7236-483c-a853-124ff009918a', 'd60915ce-413f-4527-82a1-f428a4faf838', 'fist-palm', '2026-02-20 17:04:35.127071', '2026-02-20 17:05:35.127071', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 17:04:35.127424');
INSERT INTO public.exercise_sets VALUES ('3ed33a14-ec67-462a-a768-e16144075619', 'd60915ce-413f-4527-82a1-f428a4faf838', 'fist-palm', '2026-02-20 17:04:35.726816', '2026-02-20 17:05:35.726816', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 17:04:35.726989');
INSERT INTO public.exercise_sets VALUES ('c13328e7-3550-4b33-bce1-836a65d83cb9', '4e4bd8a5-1255-4401-8589-07927e41813a', 'fist-palm', '2026-02-20 17:04:43.196811', '2026-02-20 17:05:43.196811', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 17:04:43.196992');
INSERT INTO public.exercise_sets VALUES ('37afec46-2723-479b-ace6-c1c4a8a13081', '4e4bd8a5-1255-4401-8589-07927e41813a', 'fist-palm', '2026-02-20 17:04:43.797632', '2026-02-20 17:05:43.797632', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 17:04:43.79781');
INSERT INTO public.exercise_sets VALUES ('78503be5-6101-493d-ba68-092cfae31a40', '066843e7-7cc3-46dd-b599-ea8379ca349f', 'fist-palm', '2026-02-20 22:46:10.673441', '2026-02-20 22:47:10.673441', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 22:46:10.673846');
INSERT INTO public.exercise_sets VALUES ('d5d18cbe-cc67-42ee-8b10-a6821367c5d1', '066843e7-7cc3-46dd-b599-ea8379ca349f', 'fist-palm', '2026-02-20 22:46:11.275239', '2026-02-20 22:47:11.275239', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 22:46:11.275512');
INSERT INTO public.exercise_sets VALUES ('0639a005-9e25-4571-b8cb-47e85ac03501', '61846537-27fc-48be-9fd2-f92bec9bccbd', 'fist-palm', '2026-02-20 22:46:20.712744', '2026-02-20 22:47:20.712744', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 22:46:20.712954');
INSERT INTO public.exercise_sets VALUES ('466bce3f-14ec-44ea-a229-ab3dcd74750b', '61846537-27fc-48be-9fd2-f92bec9bccbd', 'fist-palm', '2026-02-20 22:46:21.310321', '2026-02-20 22:47:21.310321', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 22:46:21.31049');
INSERT INTO public.exercise_sets VALUES ('c5bb0fe8-539f-4f3d-b7e3-9fcc9372f0fa', '61846537-27fc-48be-9fd2-f92bec9bccbd', 'fist-palm', '2026-02-20 22:46:21.911072', '2026-02-20 22:47:21.911072', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 22:46:21.911331');
INSERT INTO public.exercise_sets VALUES ('f19d7ffe-7490-4d65-9186-2bd832df7332', '61846537-27fc-48be-9fd2-f92bec9bccbd', 'fist-palm', '2026-02-20 22:46:22.510061', '2026-02-20 22:47:22.510061', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 22:46:22.510344');
INSERT INTO public.exercise_sets VALUES ('268e1645-5978-438d-8b50-c36fefa6a04d', '61846537-27fc-48be-9fd2-f92bec9bccbd', 'fist-palm', '2026-02-20 22:46:23.111762', '2026-02-20 22:47:23.111762', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 22:46:23.11209');
INSERT INTO public.exercise_sets VALUES ('74ca6cd8-5a5e-4d0c-a89a-cd00af17a9b2', '61846537-27fc-48be-9fd2-f92bec9bccbd', 'fist-palm', '2026-02-20 22:46:23.709459', '2026-02-20 22:47:23.709459', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 22:46:23.709634');
INSERT INTO public.exercise_sets VALUES ('b7c6dd1a-5d5a-48a8-bbf1-df0504c15286', '61846537-27fc-48be-9fd2-f92bec9bccbd', 'fist-palm', '2026-02-20 22:46:24.308945', '2026-02-20 22:47:24.308945', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 22:46:24.309174');
INSERT INTO public.exercise_sets VALUES ('177e1ecd-d666-4708-823f-970154108645', '3eb0ff3f-9fd0-4727-9d43-60d0cc2c505e', 'fist-palm', '2026-02-20 23:31:35.58403', '2026-02-20 23:32:35.58403', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-02-20 23:31:35.584294');
INSERT INTO public.exercise_sets VALUES ('96003d7e-3973-4a47-a0ec-873b1772565e', '528e1001-90be-44c8-9354-67f8715dc687', 'fist', '2026-02-26 14:45:22.658041', '2026-02-26 14:45:52.658041', NULL, 10, NULL, 30, 95.00, 'completed', NULL, NULL, '2026-02-26 14:45:22.65834');
INSERT INTO public.exercise_sets VALUES ('bbf58987-fc9c-452b-a58e-af1a8aa346a1', '528e1001-90be-44c8-9354-67f8715dc687', 'fist-index', '2026-02-26 14:45:23.17364', '2026-02-26 14:45:48.17364', NULL, 8, NULL, 25, 88.00, 'completed', NULL, NULL, '2026-02-26 14:45:23.173939');
INSERT INTO public.exercise_sets VALUES ('6594ecac-6ac4-4b74-a70a-9540699f9966', '528e1001-90be-44c8-9354-67f8715dc687', 'fist-palm', '2026-02-26 14:45:23.689608', '2026-02-26 14:46:23.689608', NULL, 5, NULL, 60, 92.00, 'completed', NULL, NULL, '2026-02-26 14:45:23.689858');
INSERT INTO public.exercise_sets VALUES ('3584ca1a-8a29-47b2-a927-ffd22548fda4', '7169d74c-e3f2-4606-9a60-5f73dd7821eb', 'fist-palm', '2026-03-01 17:56:39.694419', '2026-03-01 17:57:39.694419', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 17:56:39.69487');
INSERT INTO public.exercise_sets VALUES ('dd65cefa-6f59-43ba-84cf-a6f61d7cb041', '7169d74c-e3f2-4606-9a60-5f73dd7821eb', 'fist-palm', '2026-03-01 17:56:48.092518', '2026-03-01 17:57:48.092518', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 17:56:48.092742');
INSERT INTO public.exercise_sets VALUES ('0f2b2a53-074d-4ee5-aeb8-8e24633130da', '7169d74c-e3f2-4606-9a60-5f73dd7821eb', 'fist-palm', '2026-03-01 17:56:57.092402', '2026-03-01 17:57:57.092402', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 17:56:57.092627');
INSERT INTO public.exercise_sets VALUES ('85168262-2e91-45d8-8273-92a5ed69a458', '7169d74c-e3f2-4606-9a60-5f73dd7821eb', 'fist-palm', '2026-03-01 17:57:06.69165', '2026-03-01 17:58:06.69165', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 17:57:06.691834');
INSERT INTO public.exercise_sets VALUES ('85c002b4-c8c1-40a9-b5c4-106d6853dce6', '7169d74c-e3f2-4606-9a60-5f73dd7821eb', 'fist-palm', '2026-03-01 17:57:15.692898', '2026-03-01 17:58:15.692898', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 17:57:15.693098');
INSERT INTO public.exercise_sets VALUES ('4058955e-a8a0-44af-82c5-d921353555f6', 'fc5560ed-53ca-4811-a557-b4d6f87f434d', 'fist-palm', '2026-03-01 17:57:42.962628', '2026-03-01 17:58:42.962628', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 17:57:42.962893');
INSERT INTO public.exercise_sets VALUES ('3fa65f1d-3c26-4798-aeb5-f61e52e48de8', 'fc5560ed-53ca-4811-a557-b4d6f87f434d', 'fist-palm', '2026-03-01 17:57:50.760753', '2026-03-01 17:58:50.760753', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 17:57:50.760931');
INSERT INTO public.exercise_sets VALUES ('08bbe38b-423d-4431-9205-f9c1ce832ff3', 'fc5560ed-53ca-4811-a557-b4d6f87f434d', 'fist-palm', '2026-03-01 17:57:59.760831', '2026-03-01 17:58:59.760831', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 17:57:59.760998');
INSERT INTO public.exercise_sets VALUES ('8dc82dcc-7bf9-4fdd-a407-94ea28a4b6cd', 'fc5560ed-53ca-4811-a557-b4d6f87f434d', 'fist-palm', '2026-03-01 17:58:09.362766', '2026-03-01 17:59:09.362766', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 17:58:09.362991');
INSERT INTO public.exercise_sets VALUES ('f6a059be-843c-40ae-95f4-8a2450648620', 'fc5560ed-53ca-4811-a557-b4d6f87f434d', 'fist-palm', '2026-03-01 17:58:17.761968', '2026-03-01 17:59:17.761968', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 17:58:17.76221');
INSERT INTO public.exercise_sets VALUES ('a166fe2e-aee0-4fa9-b00f-8bfa128e486a', '2cdebb4b-0394-41ba-bb3b-3c09f85aa3df', 'fist-palm', '2026-03-01 18:49:44.458237', '2026-03-01 18:50:44.458237', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 18:49:44.45851');
INSERT INTO public.exercise_sets VALUES ('bb1bf24f-6496-4eba-ac06-ac541ef329b3', '2cdebb4b-0394-41ba-bb3b-3c09f85aa3df', 'fist-palm', '2026-03-01 18:49:53.456524', '2026-03-01 18:50:53.456524', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 18:49:53.456821');
INSERT INTO public.exercise_sets VALUES ('6135b297-fac8-4075-b766-e8a6e5ec1444', '2cdebb4b-0394-41ba-bb3b-3c09f85aa3df', 'fist-palm', '2026-03-01 18:50:01.854519', '2026-03-01 18:51:01.854519', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 18:50:01.854817');
INSERT INTO public.exercise_sets VALUES ('5b5396b8-73b6-4377-afa7-7a0d95d6cc5e', '2cdebb4b-0394-41ba-bb3b-3c09f85aa3df', 'fist-palm', '2026-03-01 18:50:10.253218', '2026-03-01 18:51:10.253218', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 18:50:10.253472');
INSERT INTO public.exercise_sets VALUES ('9debce37-69d6-4e8b-a686-cfbbcfd049eb', '2cdebb4b-0394-41ba-bb3b-3c09f85aa3df', 'fist-palm', '2026-03-01 18:50:27.650722', '2026-03-01 18:51:27.650722', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 18:50:27.650881');
INSERT INTO public.exercise_sets VALUES ('3c555af7-b30d-4d75-8301-45cf587798c4', '2cdebb4b-0394-41ba-bb3b-3c09f85aa3df', 'fist-palm', '2026-03-01 18:50:36.6461', '2026-03-01 18:51:36.6461', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 18:50:36.646331');
INSERT INTO public.exercise_sets VALUES ('2ef93c6b-bdc2-4b9c-8b7d-4fac7aca9e98', '2cdebb4b-0394-41ba-bb3b-3c09f85aa3df', 'fist-palm', '2026-03-01 18:50:45.644788', '2026-03-01 18:51:45.644788', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 18:50:45.645081');
INSERT INTO public.exercise_sets VALUES ('4597dc44-c052-4d28-97ad-715084a6bd45', '2cdebb4b-0394-41ba-bb3b-3c09f85aa3df', 'fist-palm', '2026-03-01 18:50:54.046172', '2026-03-01 18:51:54.046172', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 18:50:54.046427');
INSERT INTO public.exercise_sets VALUES ('ea952722-6410-4aa9-9368-c45441fd50e2', 'f6113b51-33b4-4284-ad67-52c2224807d7', 'fist-palm', '2026-03-01 18:55:01.230226', '2026-03-01 18:56:01.230226', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 18:55:01.230481');
INSERT INTO public.exercise_sets VALUES ('6bf0d755-2c42-4401-9540-7503ec2ff056', 'f6113b51-33b4-4284-ad67-52c2224807d7', 'fist-palm', '2026-03-01 18:55:09.628727', '2026-03-01 18:56:09.628727', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 18:55:09.6289');
INSERT INTO public.exercise_sets VALUES ('b2b9a890-7906-47cc-8d81-e02b254f5b3f', 'f6113b51-33b4-4284-ad67-52c2224807d7', 'fist-palm', '2026-03-01 18:55:18.627994', '2026-03-01 18:56:18.627994', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 18:55:18.62817');
INSERT INTO public.exercise_sets VALUES ('22079b06-b60c-4945-8a4d-007a7d25dd08', 'f6113b51-33b4-4284-ad67-52c2224807d7', 'fist-palm', '2026-03-01 18:55:27.027717', '2026-03-01 18:56:27.027717', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 18:55:27.027976');
INSERT INTO public.exercise_sets VALUES ('90763610-1f63-4c23-921d-871498aff1e7', '812e8270-322b-4c05-bcc7-173cb3d110e5', 'fist-palm', '2026-03-01 19:02:31.389544', '2026-03-01 19:03:31.389544', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 19:02:31.389712');
INSERT INTO public.exercise_sets VALUES ('33dfcb57-4326-4eb6-92f5-72154761d2c6', '218c6235-4a7a-4567-9406-aff366c667f3', 'fist-palm', '2026-03-01 19:04:40.523847', '2026-03-01 19:05:40.523847', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 19:04:40.524068');
INSERT INTO public.exercise_sets VALUES ('49e821c6-3582-4285-baac-18031332741e', '218c6235-4a7a-4567-9406-aff366c667f3', 'fist-palm', '2026-03-01 19:04:48.324551', '2026-03-01 19:05:48.324551', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 19:04:48.324819');
INSERT INTO public.exercise_sets VALUES ('6767ddfa-f5d5-4259-ab74-f7ed65ede914', '218c6235-4a7a-4567-9406-aff366c667f3', 'fist-palm', '2026-03-01 19:04:56.724273', '2026-03-01 19:05:56.724273', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 19:04:56.724451');
INSERT INTO public.exercise_sets VALUES ('4cbaa7f2-b062-424a-ba92-dc733cf0959a', '218c6235-4a7a-4567-9406-aff366c667f3', 'fist-palm', '2026-03-01 19:05:06.324268', '2026-03-01 19:06:06.324268', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 19:05:06.324509');
INSERT INTO public.exercise_sets VALUES ('79d3aeac-58a9-4ef6-b6e7-87923d81cff1', '218c6235-4a7a-4567-9406-aff366c667f3', 'fist-palm', '2026-03-01 19:05:06.336352', '2026-03-01 19:06:06.336352', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 19:05:06.336462');
INSERT INTO public.exercise_sets VALUES ('7e058324-9b85-4f5b-8297-5a69d72a27f9', 'd8871c72-96f9-49b9-a741-b11f873a2331', 'fist-palm', '2026-03-01 19:08:12.827328', '2026-03-01 19:09:12.827328', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 19:08:12.827661');
INSERT INTO public.exercise_sets VALUES ('701065eb-9db1-4f60-a85b-adf2d53063a7', 'd8871c72-96f9-49b9-a741-b11f873a2331', 'fist-palm', '2026-03-01 19:08:21.227163', '2026-03-01 19:09:21.227163', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 19:08:21.227398');
INSERT INTO public.exercise_sets VALUES ('1c9445e5-9509-44ca-80c4-43adb898c1f6', 'd8871c72-96f9-49b9-a741-b11f873a2331', 'fist-palm', '2026-03-01 19:08:29.627417', '2026-03-01 19:09:29.627417', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 19:08:29.627826');
INSERT INTO public.exercise_sets VALUES ('8c1de277-8577-4974-9d3f-3341216067f3', 'd8871c72-96f9-49b9-a741-b11f873a2331', 'fist-palm', '2026-03-01 19:08:37.429633', '2026-03-01 19:09:37.429633', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 19:08:37.429802');
INSERT INTO public.exercise_sets VALUES ('30dd860a-aa51-4faa-8f37-485ed364a4fd', 'd8871c72-96f9-49b9-a741-b11f873a2331', 'fist-palm', '2026-03-01 19:08:37.454939', '2026-03-01 19:09:37.454939', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-01 19:08:37.455103');
INSERT INTO public.exercise_sets VALUES ('50cc3a32-6cdd-408e-b4fa-d5479a7d5fc6', 'fa402ade-337c-4285-b5df-c4fb50c1c45e', 'fist-palm', '2026-03-03 18:30:02.229253', '2026-03-03 18:31:02.229253', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-03 18:30:02.229666');
INSERT INTO public.exercise_sets VALUES ('6ae1966d-41c8-4a86-9b43-ca5e4fb72bd6', 'd19e885e-6b6e-4d90-ae4f-a06e8a091443', 'fist-palm', '2026-03-03 18:32:16.258544', '2026-03-03 18:33:16.258544', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-03 18:32:16.259221');
INSERT INTO public.exercise_sets VALUES ('8a437025-6902-4faa-abd8-fa37f630849b', '4e5b458e-d308-42f8-8d92-3be8d3c90576', 'fist-palm', '2026-03-03 20:29:33.818242', '2026-03-03 20:30:33.818242', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-03 20:29:33.818954');
INSERT INTO public.exercise_sets VALUES ('472ea901-3c41-4003-a6ef-60173402e2c1', '80f668c1-0b9c-4050-9824-04b196ab0200', 'fist-palm', '2026-03-03 20:40:17.081607', '2026-03-03 20:41:17.081607', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-03 20:40:17.082275');
INSERT INTO public.exercise_sets VALUES ('3aafe26c-c7fe-4035-a694-7aeb3992d34f', '305412b0-9b53-4f1e-87b0-2c77f17d83b1', 'fist-palm', '2026-03-03 21:58:39.995047', '2026-03-03 21:59:39.995047', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-03 21:58:39.995894');
INSERT INTO public.exercise_sets VALUES ('f9b9ced7-615b-449a-a443-cef224bfb43b', '7f8ffa2e-0893-4348-a9b6-3dd59e759eb1', 'fist-palm', '2026-03-03 21:58:53.95063', '2026-03-03 21:59:53.95063', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-03 21:58:53.959635');
INSERT INTO public.exercise_sets VALUES ('4202cfc1-b9c9-43b1-a07d-2433eb718c02', 'a9950b65-633c-4b81-a2e9-e8bc9d4f9279', 'fist-palm', '2026-03-04 17:41:22.918157', '2026-03-04 17:42:22.918157', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-04 17:41:22.918494');
INSERT INTO public.exercise_sets VALUES ('b6a8e975-ce93-4a0a-89c8-65307917e258', 'ee575410-8b36-4b35-be9a-e05c2981ccac', 'fist-palm', '2026-03-04 18:23:46.776985', '2026-03-04 18:24:46.776985', NULL, 5, NULL, 60, 95.00, 'completed', NULL, NULL, '2026-03-04 18:23:46.781166');


--
-- Data for Name: exercise_stats; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.exercise_stats VALUES ('abd4676d-e081-4863-9870-f4f0e59462a8', '643b6d41-9df6-4e48-ac9b-12eb5e714b85', 'fist-palm', 2, 10, 120, 95.00, 93.50, '2026-03-03 20:40:17.092759', '2026-02-26 14:45:23.700102', '2026-03-03 20:40:17.09251');
INSERT INTO public.exercise_stats VALUES ('8b5e9765-ab52-4b0e-934f-adbcd29163b9', '9b8d85a4-b6c2-4839-a70b-5f597496af02', 'fist-palm', 18, 90, 1080, 95.00, 95.00, '2026-03-04 18:23:46.786842', '2026-02-20 23:31:35.585538', '2026-03-04 18:23:46.786804');
INSERT INTO public.exercise_stats VALUES ('ab1953ad-f430-4aa3-938d-9cdcd81436bb', 'adaf71fa-100e-4ad6-af29-e582414908a2', 'fist-palm', 70, 350, 4200, 95.00, 95.00, '2026-02-20 22:46:24.317971', '2026-02-20 16:36:34.649751', '2026-02-20 22:46:24.317925');
INSERT INTO public.exercise_stats VALUES ('c139ca11-8733-412b-818c-7ed64ac0d38f', '643b6d41-9df6-4e48-ac9b-12eb5e714b85', 'fist', 1, 10, 30, 95.00, 95.00, '2026-02-26 14:45:22.662564', '2026-02-26 14:45:22.662518', '2026-02-26 14:45:22.662518');
INSERT INTO public.exercise_stats VALUES ('99e17877-9d85-4796-b789-ff94fdb8dd77', '643b6d41-9df6-4e48-ac9b-12eb5e714b85', 'fist-index', 1, 8, 25, 88.00, 88.00, '2026-02-26 14:45:23.184339', '2026-02-26 14:45:23.184247', '2026-02-26 14:45:23.184247');
INSERT INTO public.exercise_stats VALUES ('a8457f09-7b1f-484f-90f7-4867ff1e4a37', 'ea47d1d7-c7a0-4e6a-b0fa-fa0551047349', 'fist-palm', 18, 90, 1080, 95.00, 95.00, '2026-03-01 19:05:06.337136', '2026-03-01 18:49:44.467276', '2026-03-01 19:05:06.337111');
INSERT INTO public.exercise_stats VALUES ('9fde219f-69cd-489c-a54b-b281ddce27a2', 'b4fd3333-1d59-421e-bb18-198df724a66d', 'fist-palm', 5, 25, 300, 95.00, 95.00, '2026-03-01 19:08:37.455754', '2026-03-01 19:08:12.836344', '2026-03-01 19:08:37.455727');


--
-- Data for Name: exercises; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.exercises VALUES ('fist', 'Кулак', 'Сжимайте и разжимайте кулаки для укрепления кистей', 1, 1, '{"Мышцы кисти","Сгибатели пальцев"}', '{"Сожмите кулак","Держите 3 секунды",Разожмите,"Повторите 10 раз"}', 30, NULL, NULL, NULL, true, '2026-02-14 00:11:01.365723', NULL);
INSERT INTO public.exercises VALUES ('fist-index', 'Кулак с указательным', 'Кулак с поднятым указательным пальцем для развития координации', 1, 2, '{"Мышцы кисти","Разгибатели пальцев"}', '{"Сожмите кулак","Поднимите указательный палец","Держите 3 секунды"}', 30, NULL, NULL, NULL, true, '2026-02-14 00:11:01.365723', NULL);
INSERT INTO public.exercises VALUES ('fist-palm', 'Кулак-ладонь', 'Чередование кулака и ладони для улучшения кровообращения', 1, 2, '{"Мышцы кисти","Мышцы предплечья"}', '{"Сожмите кулак","Держите 3 секунды","Раскройте ладонь","Держите 3 секунды"}', 60, NULL, NULL, NULL, true, '2026-02-14 00:11:01.365723', NULL);
INSERT INTO public.exercises VALUES ('finger-touching', 'Считалочка', 'Поочередное касание пальцев - классическое упражнение для развития мелкой моторики и координации движений. По очереди соединяйте большой палец с указательным, средним, безымянным и мизинцем.', 4, 2, '{"Мышцы кисти","Сгибатели пальцев",Мышцы-противопоставители}', '{"Соедините подушечку большого пальца с указательным","Верните большой палец в исходное положение","Соедините большой палец со средним","Повторите с безымянным пальцем","Завершите касанием с мизинцем","Повторите цикл 5-10 раз"}', 45, 2.50, NULL, NULL, true, '2026-03-04 15:12:01.3187', '{"tips": ["Старайтесь делать упражнение плавно", "Следите чтобы другие пальцы не сгибались", "Дышите ровно, не задерживайте дыхание"], "purpose": "Развитие мелкой моторики и нейропластичности", "benefits": ["Улучшение координации движений", "Развитие нервных связей между полушариями", "Профилактика туннельного синдрома", "Улучшение кровообращения в кистях"], "variations": ["Ускорить темп", "Делать с закрытыми глазами", "Выполнять одновременно двумя руками"], "contraindications": ["Травмы пальцев в острой фазе", "Воспалительные процессы в суставах"]}');


--
-- Data for Name: overall_stats; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.overall_stats VALUES ('adaf71fa-100e-4ad6-af29-e582414908a2', 70, 70, 350, 4200, 1, 1, 1, '2026-02-20 16:36:34.649751', '2026-02-20 22:46:24.317971', '2026-02-20 16:36:34.649751', '2026-02-20 22:46:24.317925');
INSERT INTO public.overall_stats VALUES ('b4fd3333-1d59-421e-bb18-198df724a66d', 5, 5, 25, 300, 1, 1, 1, '2026-03-01 19:08:12.836344', '2026-03-01 19:08:37.455754', '2026-03-01 19:08:12.836344', '2026-03-01 19:08:37.455727');
INSERT INTO public.overall_stats VALUES ('643b6d41-9df6-4e48-ac9b-12eb5e714b85', 4, 4, 28, 175, 3, 1, 1, '2026-02-26 14:45:22.662518', '2026-03-03 20:40:17.092759', '2026-02-26 14:45:22.662518', '2026-03-03 20:40:17.09251');
INSERT INTO public.overall_stats VALUES ('9b8d85a4-b6c2-4839-a70b-5f597496af02', 18, 18, 90, 1080, 1, 3, 3, '2026-02-20 23:31:35.585538', '2026-03-04 18:23:46.786842', '2026-02-20 23:31:35.585538', '2026-03-04 18:23:46.786804');
INSERT INTO public.overall_stats VALUES ('ea47d1d7-c7a0-4e6a-b0fa-fa0551047349', 18, 18, 90, 1080, 1, 1, 1, '2026-03-01 18:49:44.467276', '2026-03-01 19:05:06.337136', '2026-03-01 18:49:44.467276', '2026-03-01 19:05:06.337111');


--
-- Data for Name: user_achievements; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: user_exercise_progress; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: user_settings; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.users VALUES ('28e4366a-599e-40f4-8fcd-08ba87bbacaf', 'testuser', 'test@example.com', '$2a$10$nTlhjW4OzeTF0VJcw1J5/OtAR2evCBxKIqjwpWNZvhXlhN1Wxr676', 'Тест', 'Пользователь', NULL, NULL, NULL, NULL, '2026-02-14 00:51:14.299442', '2026-02-14 01:15:36.761254', '2026-02-14 01:15:36.761254', true, 'user');
INSERT INTO public.users VALUES ('b4fd3333-1d59-421e-bb18-198df724a66d', 'asd', 'a@a.aaa', '$2a$10$FpbK1bc7GBbcy1E2gHS.ou2728OF6H4qkVcLjQcT4/IXRxiQrGP4.', 'asd', 'asd', NULL, NULL, NULL, NULL, '2026-03-01 19:07:04.516934', '2026-03-01 19:07:31.361295', '2026-03-01 19:07:31.361295', true, 'user');
INSERT INTO public.users VALUES ('adaf71fa-100e-4ad6-af29-e582414908a2', 'test', 't@t.com', '$2a$10$8QQo7fnsBRC3nGeadx4hg.bWNimkxKr1JRP1N7cRKEUYFMYK8q6Aq', 'n', 'n', NULL, NULL, NULL, NULL, '2026-02-20 16:26:26.60906', '2026-02-24 13:26:44.548796', '2026-02-24 13:26:44.548796', true, 'user');
INSERT INTO public.users VALUES ('643b6d41-9df6-4e48-ac9b-12eb5e714b85', 'test2', 'test@test.com', '$2a$10$OwYmqmW0RCOlns0oB/nvTuCfDdwiKBCEABcZCz145rSzoV7NM3Q.G', 'Name', 'FName', NULL, NULL, NULL, NULL, '2026-02-16 13:49:50.200577', '2026-03-06 13:13:14.058347', '2026-03-06 13:13:14.058347', true, 'user');
INSERT INTO public.users VALUES ('9b8d85a4-b6c2-4839-a70b-5f597496af02', 'e@e.com', 'e@e.com', '$2a$10$qH68/HBsmr//UEmgSVxoJOgvpKl6qwzka/YQdf.Y8ilIcK71o6RQq', 'qw', 'qw', NULL, NULL, NULL, NULL, '2026-02-20 22:55:46.934536', '2026-03-06 14:20:25.688148', '2026-03-06 14:20:25.688148', true, 'user');
INSERT INTO public.users VALUES ('ea47d1d7-c7a0-4e6a-b0fa-fa0551047349', 'qwe', 'q@q.com', '$2a$10$U6imX211h9wIfwjjlUTz7eiFEOZSLarlRJx.UewAb73nsZjm5HAVe', 'qwe', 'qwe', NULL, NULL, NULL, NULL, '2026-03-01 18:48:57.802531', '2026-03-01 19:04:19.604348', '2026-03-01 19:04:19.604348', true, 'user');


--
-- Data for Name: workout_sessions; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.workout_sessions VALUES ('a2b9b130-ae5f-4987-bf47-64a08252d265', '643b6d41-9df6-4e48-ac9b-12eb5e714b85', '2026-02-20 15:27:13.613143', '2026-02-20 15:27:15.149532', 2, 'completed', NULL, '2026-02-20 15:27:13.613581');
INSERT INTO public.workout_sessions VALUES ('fa2550bd-90c9-45d0-9102-d2d1784c4101', '643b6d41-9df6-4e48-ac9b-12eb5e714b85', '2026-02-20 15:27:15.152632', '2026-02-20 15:27:16.686756', 2, 'completed', NULL, '2026-02-20 15:27:15.152758');
INSERT INTO public.workout_sessions VALUES ('53c6c402-f95f-4abb-ac9b-8c674ba3bf5f', '643b6d41-9df6-4e48-ac9b-12eb5e714b85', '2026-02-20 15:27:16.688354', '2026-02-20 15:27:18.221936', 2, 'completed', NULL, '2026-02-20 15:27:16.688432');
INSERT INTO public.workout_sessions VALUES ('2d67fc44-e5b5-46b5-9e03-15a5ad273e3f', '643b6d41-9df6-4e48-ac9b-12eb5e714b85', '2026-02-20 16:09:22.594003', '2026-02-20 16:09:24.130997', 2, 'completed', NULL, '2026-02-20 16:09:22.594441');
INSERT INTO public.workout_sessions VALUES ('0cb81407-962d-4e48-9fc2-ab5bcf0349a5', '643b6d41-9df6-4e48-ac9b-12eb5e714b85', '2026-02-20 16:09:24.134184', '2026-02-20 16:09:25.667929', 2, 'completed', NULL, '2026-02-20 16:09:24.13437');
INSERT INTO public.workout_sessions VALUES ('3ff9aacf-1b2a-4c72-8c91-993f95354cfb', '643b6d41-9df6-4e48-ac9b-12eb5e714b85', '2026-02-20 16:09:25.669781', '2026-02-20 16:09:27.203582', 2, 'completed', NULL, '2026-02-20 16:09:25.669867');
INSERT INTO public.workout_sessions VALUES ('2d40a994-0840-457e-b137-5a5acebc5591', 'adaf71fa-100e-4ad6-af29-e582414908a2', '2026-02-20 16:35:50.469284', '2026-02-20 16:36:34.651423', 44, 'completed', NULL, '2026-02-20 16:35:50.469621');
INSERT INTO public.workout_sessions VALUES ('7e43b46f-fffc-4ac2-b393-863576ca914e', 'adaf71fa-100e-4ad6-af29-e582414908a2', '2026-02-20 16:37:06.34227', '2026-02-20 16:37:14.257312', 8, 'completed', NULL, '2026-02-20 16:37:06.342458');
INSERT INTO public.workout_sessions VALUES ('7bb695c6-9398-4cc0-9473-a42cc57b4bab', 'adaf71fa-100e-4ad6-af29-e582414908a2', '2026-02-20 16:41:42.056909', '2026-02-20 16:42:23.888235', 42, 'completed', NULL, '2026-02-20 16:41:42.057117');
INSERT INTO public.workout_sessions VALUES ('cbee99c3-f939-4940-b0be-b0a205b4b68f', 'adaf71fa-100e-4ad6-af29-e582414908a2', '2026-02-20 16:42:25.907234', '2026-02-20 16:42:30.864593', 5, 'completed', NULL, '2026-02-20 16:42:25.907409');
INSERT INTO public.workout_sessions VALUES ('a4e89c7c-1125-4dd7-b3ae-55b23e02adde', 'adaf71fa-100e-4ad6-af29-e582414908a2', '2026-02-20 16:43:02.912362', '2026-02-20 16:43:14.322977', 11, 'completed', NULL, '2026-02-20 16:43:02.91258');
INSERT INTO public.workout_sessions VALUES ('3f775be0-98c8-4c98-8635-b188066f15a3', 'adaf71fa-100e-4ad6-af29-e582414908a2', '2026-02-20 16:49:41.17505', '2026-02-20 16:50:25.978107', 45, 'completed', NULL, '2026-02-20 16:49:41.175207');
INSERT INTO public.workout_sessions VALUES ('64fa6ac8-a9b2-4d1c-8770-1c7b16f4deb5', 'adaf71fa-100e-4ad6-af29-e582414908a2', '2026-02-20 16:50:30.592773', '2026-02-20 16:50:34.981506', 4, 'completed', NULL, '2026-02-20 16:50:30.59297');
INSERT INTO public.workout_sessions VALUES ('8bb68f6a-816b-421e-b62a-b956ef926542', 'adaf71fa-100e-4ad6-af29-e582414908a2', '2026-02-20 16:54:56.755493', '2026-02-20 16:55:40.565182', 44, 'completed', NULL, '2026-02-20 16:54:56.755751');
INSERT INTO public.workout_sessions VALUES ('d98699a0-0264-4e8e-818b-11bbb2004f0d', 'adaf71fa-100e-4ad6-af29-e582414908a2', '2026-02-20 16:55:46.374771', '2026-02-20 16:55:50.76423', 4, 'completed', NULL, '2026-02-20 16:55:46.374932');
INSERT INTO public.workout_sessions VALUES ('547c2aff-a5dd-4ee3-9462-b62f51b2cd0d', 'adaf71fa-100e-4ad6-af29-e582414908a2', '2026-02-20 16:57:09.113181', '2026-02-20 16:57:51.317854', 42, 'completed', NULL, '2026-02-20 16:57:09.11351');
INSERT INTO public.workout_sessions VALUES ('f52ba3d9-f1c4-40cd-8cf3-ab8074d2e84d', 'adaf71fa-100e-4ad6-af29-e582414908a2', '2026-02-20 16:57:53.928634', '2026-02-20 16:57:59.502808', 6, 'completed', NULL, '2026-02-20 16:57:53.928856');
INSERT INTO public.workout_sessions VALUES ('d3ecac06-a77b-40f8-9b05-06e3590871ff', 'adaf71fa-100e-4ad6-af29-e582414908a2', '2026-02-20 16:59:21.676193', '2026-02-20 17:00:07.161933', 45, 'completed', NULL, '2026-02-20 16:59:21.676407');
INSERT INTO public.workout_sessions VALUES ('339bc908-ab71-40c3-8b20-6e7c7f29b496', 'adaf71fa-100e-4ad6-af29-e582414908a2', '2026-02-20 17:00:09.305097', '2026-02-20 17:00:13.402042', 4, 'completed', NULL, '2026-02-20 17:00:09.305268');
INSERT INTO public.workout_sessions VALUES ('d60915ce-413f-4527-82a1-f428a4faf838', 'adaf71fa-100e-4ad6-af29-e582414908a2', '2026-02-20 17:03:55.519171', '2026-02-20 17:04:37.4053', 42, 'completed', NULL, '2026-02-20 17:03:55.519351');
INSERT INTO public.workout_sessions VALUES ('4e4bd8a5-1255-4401-8589-07927e41813a', 'adaf71fa-100e-4ad6-af29-e582414908a2', '2026-02-20 17:04:40.789631', '2026-02-20 17:04:44.999538', 4, 'completed', NULL, '2026-02-20 17:04:40.7898');
INSERT INTO public.workout_sessions VALUES ('066843e7-7cc3-46dd-b599-ea8379ca349f', 'adaf71fa-100e-4ad6-af29-e582414908a2', '2026-02-20 22:45:26.390659', '2026-02-20 22:46:13.979883', 48, 'completed', NULL, '2026-02-20 22:45:26.399156');
INSERT INTO public.workout_sessions VALUES ('61846537-27fc-48be-9fd2-f92bec9bccbd', 'adaf71fa-100e-4ad6-af29-e582414908a2', '2026-02-20 22:46:17.691314', '2026-02-20 22:46:24.387486', 7, 'completed', NULL, '2026-02-20 22:46:17.691535');
INSERT INTO public.workout_sessions VALUES ('14a714d3-1fba-4387-bec9-ed95cf22912f', 'adaf71fa-100e-4ad6-af29-e582414908a2', '2026-02-20 22:52:17.53314', '2026-02-20 22:54:14.280076', 117, 'completed', NULL, '2026-02-20 22:52:17.533376');
INSERT INTO public.workout_sessions VALUES ('76b938db-15e3-4759-b84a-4032ee8c8a90', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-20 22:56:24.338781', '2026-02-20 22:57:07.2415', 43, 'completed', NULL, '2026-02-20 22:56:24.338972');
INSERT INTO public.workout_sessions VALUES ('c2b9e372-ff5d-4854-ad43-e60cc3f67710', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-20 23:01:10.753079', '2026-02-20 23:01:55.668167', 45, 'completed', NULL, '2026-02-20 23:01:10.753256');
INSERT INTO public.workout_sessions VALUES ('91281582-b766-4b69-b6c4-9e8191c63aab', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-20 23:12:10.807878', '2026-02-20 23:12:49.22398', 38, 'completed', NULL, '2026-02-20 23:12:10.808066');
INSERT INTO public.workout_sessions VALUES ('e7c10d32-ab7e-4771-9fe4-2dfd2e988330', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-20 23:13:02.172928', '2026-02-20 23:13:15.262632', 13, 'completed', NULL, '2026-02-20 23:13:02.173132');
INSERT INTO public.workout_sessions VALUES ('fc11a0e2-37ce-496e-b834-5eefbfe8b085', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-20 23:24:36.554678', '2026-02-20 23:25:22.049609', 45, 'completed', NULL, '2026-02-20 23:24:36.554849');
INSERT INTO public.workout_sessions VALUES ('3eb0ff3f-9fd0-4727-9d43-60d0cc2c505e', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-20 23:31:35.581497', '2026-02-20 23:31:35.58716', 0, 'completed', NULL, '2026-02-20 23:31:35.581782');
INSERT INTO public.workout_sessions VALUES ('55e54ff4-20e4-44d0-85df-7e90c82b00a9', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-26 14:39:20.787596', NULL, NULL, 'in_progress', NULL, '2026-02-26 14:39:20.788297');
INSERT INTO public.workout_sessions VALUES ('f2c0728c-550d-430d-b5ca-3f4ad562e616', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-26 14:39:22.794892', NULL, NULL, 'in_progress', NULL, '2026-02-26 14:39:22.795182');
INSERT INTO public.workout_sessions VALUES ('4f51eae5-3c4b-4b38-92a3-51ff817a93f2', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-26 14:39:24.413908', NULL, NULL, 'in_progress', NULL, '2026-02-26 14:39:24.414103');
INSERT INTO public.workout_sessions VALUES ('cd1e6aa4-376c-4b1d-8baa-54852afa5446', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-26 14:39:32.999673', '2026-02-26 14:39:53.956857', 21, 'completed', NULL, '2026-02-26 14:39:33.000034');
INSERT INTO public.workout_sessions VALUES ('82abb06e-0f7a-46f1-89fd-f263f26b98d7', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-26 14:42:46.043668', '2026-02-26 14:43:29.821789', 44, 'completed', NULL, '2026-02-26 14:42:46.0439');
INSERT INTO public.workout_sessions VALUES ('528e1001-90be-44c8-9354-67f8715dc687', '643b6d41-9df6-4e48-ac9b-12eb5e714b85', '2026-02-26 14:45:22.653699', '2026-02-26 14:45:24.205602', 2, 'completed', NULL, '2026-02-26 14:45:22.653925');
INSERT INTO public.workout_sessions VALUES ('6373196e-cee7-4923-ad56-f91c140e65ed', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-26 14:50:16.343277', NULL, NULL, 'in_progress', NULL, '2026-02-26 14:50:16.343514');
INSERT INTO public.workout_sessions VALUES ('6be385e7-ac3b-4b6c-ab16-bc9dac2f4430', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-26 14:56:03.140116', NULL, NULL, 'in_progress', NULL, '2026-02-26 14:56:03.140292');
INSERT INTO public.workout_sessions VALUES ('6fde2e10-7a87-43d6-aabc-9360b7fab0c3', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-26 15:50:34.609564', '2026-02-26 15:51:44.048915', 69, 'completed', NULL, '2026-02-26 15:50:34.60975');
INSERT INTO public.workout_sessions VALUES ('eef592db-93cd-4341-95d3-c96d57d72466', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-26 15:55:03.777911', '2026-02-26 15:55:50.925984', 47, 'completed', NULL, '2026-02-26 15:55:03.778114');
INSERT INTO public.workout_sessions VALUES ('a0703c63-e29c-4ed4-9b6d-a22eb7bd801e', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-26 15:59:16.207082', '2026-02-26 16:00:00.653354', 44, 'completed', NULL, '2026-02-26 15:59:16.207313');
INSERT INTO public.workout_sessions VALUES ('a8cd430d-33e1-446d-b557-0689c09382cc', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-26 16:04:27.484982', '2026-02-26 16:05:10.023664', 43, 'completed', NULL, '2026-02-26 16:04:27.485167');
INSERT INTO public.workout_sessions VALUES ('39996d52-ba5f-48f1-863c-c2b2cfbe396c', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-26 16:08:30.715205', '2026-02-26 16:09:20.456184', 50, 'completed', NULL, '2026-02-26 16:08:30.715368');
INSERT INTO public.workout_sessions VALUES ('6534999b-cab4-4f31-a0d2-a1a047972978', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-26 16:13:11.563595', '2026-02-26 16:14:00.327826', 49, 'completed', NULL, '2026-02-26 16:13:11.56376');
INSERT INTO public.workout_sessions VALUES ('5446eb61-20d7-4e5d-b8b8-3ae504dbfd2e', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-26 16:15:10.846039', '2026-02-26 16:15:50.872049', 40, 'completed', NULL, '2026-02-26 16:15:10.846255');
INSERT INTO public.workout_sessions VALUES ('79f0f691-5bd2-4679-a751-c9d2a3f249dc', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-26 16:18:34.829895', '2026-02-26 16:19:15.077538', 40, 'completed', NULL, '2026-02-26 16:18:34.830435');
INSERT INTO public.workout_sessions VALUES ('a69cf35d-7c41-435b-97bc-e751204ce366', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-26 16:20:08.847168', '2026-02-26 16:20:16.107756', 7, 'completed', NULL, '2026-02-26 16:20:08.847361');
INSERT INTO public.workout_sessions VALUES ('aa39f2c5-629e-44a7-af34-d4fe61e581d5', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-26 16:28:41.931319', '2026-02-26 16:29:28.0719', 46, 'completed', NULL, '2026-02-26 16:28:41.931559');
INSERT INTO public.workout_sessions VALUES ('180f9718-4eda-4720-921c-f4a05adb7843', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-26 16:36:07.113833', '2026-02-26 16:36:51.885022', 45, 'completed', NULL, '2026-02-26 16:36:07.11401');
INSERT INTO public.workout_sessions VALUES ('87f72a6d-5dc0-41da-bf33-29509277a897', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-26 16:40:41.113023', '2026-02-26 16:41:03.681844', 23, 'completed', NULL, '2026-02-26 16:40:41.113187');
INSERT INTO public.workout_sessions VALUES ('a459b1e4-a390-4db7-90be-d559a45b6368', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-26 16:41:19.752179', '2026-02-26 16:41:46.917379', 27, 'completed', NULL, '2026-02-26 16:41:19.752335');
INSERT INTO public.workout_sessions VALUES ('adcd2c56-ce6c-4c1a-86a8-ff923f5fc960', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-26 16:41:52.949273', '2026-02-26 16:41:57.636526', 5, 'completed', NULL, '2026-02-26 16:41:52.949434');
INSERT INTO public.workout_sessions VALUES ('4a0b1e1c-c646-43a0-859b-2075ecfe5593', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-26 16:44:32.403466', '2026-02-26 16:45:16.637055', 44, 'completed', NULL, '2026-02-26 16:44:32.403621');
INSERT INTO public.workout_sessions VALUES ('c6690f72-7372-4492-9cb2-06ddfc644abe', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-26 16:45:48.636255', '2026-02-26 16:46:30.083676', 41, 'completed', NULL, '2026-02-26 16:45:48.636451');
INSERT INTO public.workout_sessions VALUES ('3b398855-6164-437b-a330-b2305057dde3', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-26 16:48:31.742092', '2026-02-26 16:48:35.595327', 4, 'completed', NULL, '2026-02-26 16:48:31.742294');
INSERT INTO public.workout_sessions VALUES ('b9f80daa-f68c-408c-81f8-5e9039717f30', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-26 16:48:38.030018', '2026-02-26 16:48:39.807641', 2, 'completed', NULL, '2026-02-26 16:48:38.030225');
INSERT INTO public.workout_sessions VALUES ('428e24d3-a980-4dc7-a79f-7ac426dc2dd1', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-26 16:58:55.56358', '2026-02-26 16:59:44.534155', 49, 'completed', NULL, '2026-02-26 16:58:55.563774');
INSERT INTO public.workout_sessions VALUES ('a72bab7b-6ee7-42c4-ac09-9e54df1e2d34', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-27 12:45:41.730649', NULL, NULL, 'in_progress', NULL, '2026-02-27 12:45:41.73912');
INSERT INTO public.workout_sessions VALUES ('862337c7-72f8-47f9-97f1-f5c3abda57d5', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-27 12:45:43.133554', NULL, NULL, 'in_progress', NULL, '2026-02-27 12:45:43.133776');
INSERT INTO public.workout_sessions VALUES ('09366511-992c-4500-8790-bd22793b180b', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-27 12:45:47.471275', NULL, NULL, 'in_progress', NULL, '2026-02-27 12:45:47.471535');
INSERT INTO public.workout_sessions VALUES ('4a0e347d-6b86-4a3f-85ad-8aa05032899e', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-27 12:54:24.940556', '2026-02-27 12:55:05.734026', 41, 'completed', NULL, '2026-02-27 12:54:24.9408');
INSERT INTO public.workout_sessions VALUES ('83c3128c-dcb1-4df3-982e-cb6d28ec9785', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-27 12:55:10.951913', '2026-02-27 12:55:12.952914', 2, 'completed', NULL, '2026-02-27 12:55:10.952076');
INSERT INTO public.workout_sessions VALUES ('2730e72c-06b9-4246-883c-d36c327a698a', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-27 12:55:17.381889', '2026-02-27 12:55:18.703579', 1, 'completed', NULL, '2026-02-27 12:55:17.382109');
INSERT INTO public.workout_sessions VALUES ('462991f4-9b1d-4bc4-9bec-a935cf167ae2', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-27 12:55:41.450457', '2026-02-27 12:55:43.454491', 2, 'completed', NULL, '2026-02-27 12:55:41.450642');
INSERT INTO public.workout_sessions VALUES ('e14d4be1-8fb1-40e9-85c0-f10f1c742958', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-27 13:06:32.366161', '2026-02-27 13:07:14.238522', 42, 'completed', NULL, '2026-02-27 13:06:32.366359');
INSERT INTO public.workout_sessions VALUES ('c35921af-5356-4288-bc73-e7940812d1f5', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-27 13:07:17.91682', '2026-02-27 13:07:19.916497', 2, 'completed', NULL, '2026-02-27 13:07:17.917048');
INSERT INTO public.workout_sessions VALUES ('071a92fe-c412-43fc-b195-5bfe0e3908db', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-27 13:14:10.603408', '2026-02-27 13:14:51.00323', 40, 'completed', NULL, '2026-02-27 13:14:10.60372');
INSERT INTO public.workout_sessions VALUES ('a349ff90-1f01-488d-8316-1e062be257d8', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-27 13:14:55.083425', NULL, NULL, 'in_progress', NULL, '2026-02-27 13:14:55.083631');
INSERT INTO public.workout_sessions VALUES ('42ddafe1-06ef-4ac7-be7c-677338863ea9', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-27 13:18:54.968132', '2026-02-27 13:19:31.823006', 37, 'completed', NULL, '2026-02-27 13:18:54.968402');
INSERT INTO public.workout_sessions VALUES ('4540d326-1ae4-45e5-8d11-365c44196172', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-27 13:19:37.394581', '2026-02-27 13:19:39.401775', 2, 'completed', NULL, '2026-02-27 13:19:37.394784');
INSERT INTO public.workout_sessions VALUES ('8bc0e896-8b68-43dc-8b16-bc6a04d69602', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-27 13:26:49.057084', NULL, NULL, 'in_progress', NULL, '2026-02-27 13:26:49.057449');
INSERT INTO public.workout_sessions VALUES ('dd5c09b5-0281-4a25-843d-daff0e348d26', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-27 13:30:26.412073', NULL, NULL, 'in_progress', NULL, '2026-02-27 13:30:26.412233');
INSERT INTO public.workout_sessions VALUES ('88569ffe-5831-4b15-a447-59bd1eed4808', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-27 13:31:21.639132', '2026-02-27 13:31:59.968208', 38, 'completed', NULL, '2026-02-27 13:31:21.639324');
INSERT INTO public.workout_sessions VALUES ('a293394f-7c45-48a7-857c-64e489fd9d80', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-27 13:32:03.371624', '2026-02-27 13:32:05.372914', 2, 'completed', NULL, '2026-02-27 13:32:03.371855');
INSERT INTO public.workout_sessions VALUES ('ebf63adc-22fe-4909-9c35-165d3782d957', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-27 13:37:06.60641', NULL, NULL, 'in_progress', NULL, '2026-02-27 13:37:06.606628');
INSERT INTO public.workout_sessions VALUES ('96aa4053-dc39-4ef7-b0dd-1769a0690dc3', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-27 13:39:33.346801', '2026-02-27 13:40:12.465503', 39, 'completed', NULL, '2026-02-27 13:39:33.346953');
INSERT INTO public.workout_sessions VALUES ('87b9976c-9917-4b85-ac46-3e2fdce4aa17', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-27 13:40:19.310643', '2026-02-27 13:40:21.220394', 2, 'completed', NULL, '2026-02-27 13:40:19.31088');
INSERT INTO public.workout_sessions VALUES ('8f523508-dc1b-4368-b23a-934a63ff0504', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-27 13:48:21.504393', NULL, NULL, 'in_progress', NULL, '2026-02-27 13:48:21.504764');
INSERT INTO public.workout_sessions VALUES ('a74887f4-c9a8-4ddc-84d2-169091f84d5c', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-27 13:49:26.552493', NULL, NULL, 'in_progress', NULL, '2026-02-27 13:49:26.552805');
INSERT INTO public.workout_sessions VALUES ('8201b8f8-5253-4936-bd64-3ff15d85ba79', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-27 13:55:39.785869', NULL, NULL, 'in_progress', NULL, '2026-02-27 13:55:39.786211');
INSERT INTO public.workout_sessions VALUES ('437ee50d-999b-4062-afaa-d36d61569947', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-27 14:00:16.37871', NULL, NULL, 'in_progress', NULL, '2026-02-27 14:00:16.378862');
INSERT INTO public.workout_sessions VALUES ('be9dbca2-f2f6-4be4-bd66-08fdd4fefeb2', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-02-27 14:03:41.2378', NULL, NULL, 'in_progress', NULL, '2026-02-27 14:03:41.238034');
INSERT INTO public.workout_sessions VALUES ('bee53625-49c2-4555-b8aa-83a0f0f8fd28', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-01 17:15:30.076715', NULL, NULL, 'in_progress', NULL, '2026-03-01 17:15:30.085049');
INSERT INTO public.workout_sessions VALUES ('4478c2a1-6ebc-4d04-91ed-b2a59efd6b2d', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-01 17:19:07.916207', '2026-03-01 17:19:48.860773', 41, 'completed', NULL, '2026-03-01 17:19:07.916415');
INSERT INTO public.workout_sessions VALUES ('b809c379-0f78-4c64-9614-1708ba3bc661', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-01 17:19:55.229467', NULL, NULL, 'in_progress', NULL, '2026-03-01 17:19:55.229734');
INSERT INTO public.workout_sessions VALUES ('d1a2e869-745c-4db9-aeb4-0b2941fd17cf', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-01 17:20:05.835805', NULL, NULL, 'in_progress', NULL, '2026-03-01 17:20:05.835972');
INSERT INTO public.workout_sessions VALUES ('76f1dc06-2cb0-4500-93dd-82f951653d45', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-01 17:25:11.368526', NULL, NULL, 'in_progress', NULL, '2026-03-01 17:25:11.368763');
INSERT INTO public.workout_sessions VALUES ('7169d74c-e3f2-4606-9a60-5f73dd7821eb', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-01 17:56:17.108222', '2026-03-01 17:57:15.706749', 59, 'completed', NULL, '2026-03-01 17:56:17.108437');
INSERT INTO public.workout_sessions VALUES ('fc5560ed-53ca-4811-a557-b4d6f87f434d', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-01 17:57:30.937277', '2026-03-01 17:58:17.774073', 47, 'completed', NULL, '2026-03-01 17:57:30.937584');
INSERT INTO public.workout_sessions VALUES ('46bc02f6-e848-475e-9262-b32022179976', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-01 17:58:30.352613', NULL, NULL, 'in_progress', NULL, '2026-03-01 17:58:30.352874');
INSERT INTO public.workout_sessions VALUES ('655640f7-5de6-43a8-85b5-bc0b899d0e65', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-01 18:18:10.794808', NULL, NULL, 'in_progress', NULL, '2026-03-01 18:18:10.794956');
INSERT INTO public.workout_sessions VALUES ('2cdebb4b-0394-41ba-bb3b-3c09f85aa3df', 'ea47d1d7-c7a0-4e6a-b0fa-fa0551047349', '2026-03-01 18:49:33.035902', NULL, NULL, 'in_progress', NULL, '2026-03-01 18:49:33.036045');
INSERT INTO public.workout_sessions VALUES ('f6113b51-33b4-4284-ad67-52c2224807d7', 'ea47d1d7-c7a0-4e6a-b0fa-fa0551047349', '2026-03-01 18:54:50.408406', '2026-03-01 18:55:35.427999', 45, 'completed', NULL, '2026-03-01 18:54:50.408594');
INSERT INTO public.workout_sessions VALUES ('812e8270-322b-4c05-bcc7-173cb3d110e5', 'ea47d1d7-c7a0-4e6a-b0fa-fa0551047349', '2026-03-01 19:01:46.366225', '2026-03-01 19:02:31.40598', 45, 'completed', NULL, '2026-03-01 19:01:46.366439');
INSERT INTO public.workout_sessions VALUES ('218c6235-4a7a-4567-9406-aff366c667f3', 'ea47d1d7-c7a0-4e6a-b0fa-fa0551047349', '2026-03-01 19:04:21.90057', '2026-03-01 19:05:06.339778', 44, 'completed', NULL, '2026-03-01 19:04:21.90071');
INSERT INTO public.workout_sessions VALUES ('d8871c72-96f9-49b9-a741-b11f873a2331', 'b4fd3333-1d59-421e-bb18-198df724a66d', '2026-03-01 19:07:53.008023', '2026-03-01 19:08:37.458632', 44, 'completed', NULL, '2026-03-01 19:07:53.008198');
INSERT INTO public.workout_sessions VALUES ('4cb1c3c3-9036-4458-80d6-5b5803820412', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-02 15:08:39.758452', '2026-03-02 15:08:49.797746', 10, 'completed', NULL, '2026-03-02 15:08:39.77979');
INSERT INTO public.workout_sessions VALUES ('dc55e309-a925-4cc6-a8d8-5f57bc7937a5', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-02 15:08:52.934077', '2026-03-02 15:09:06.990834', 14, 'completed', NULL, '2026-03-02 15:08:52.934335');
INSERT INTO public.workout_sessions VALUES ('bd6bbd93-7699-4df0-a3c1-04748b7d6801', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-02 15:15:29.413421', '2026-03-02 15:15:35.137072', 6, 'completed', NULL, '2026-03-02 15:15:29.413649');
INSERT INTO public.workout_sessions VALUES ('91a76563-ae14-4e9c-8bbb-9a19b9fadfc9', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-02 15:15:35.296607', '2026-03-02 15:15:36.796724', 2, 'completed', NULL, '2026-03-02 15:15:35.296823');
INSERT INTO public.workout_sessions VALUES ('fe9fc29c-0ecd-4758-a293-4c71c520a88d', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-02 15:15:39.535648', '2026-03-02 15:15:40.837639', 1, 'completed', NULL, '2026-03-02 15:15:39.536202');
INSERT INTO public.workout_sessions VALUES ('c6b811a2-9184-4d96-9010-042a8721b9ab', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-02 15:26:12.351468', '2026-03-02 15:26:23.975267', 12, 'completed', NULL, '2026-03-02 15:26:12.351663');
INSERT INTO public.workout_sessions VALUES ('9e27d8f4-c794-49c7-9f3b-65d3c3839c83', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-02 15:29:17.661332', '2026-03-02 15:29:21.991638', 4, 'completed', NULL, '2026-03-02 15:29:17.661587');
INSERT INTO public.workout_sessions VALUES ('7dc5a54a-de1b-4b0c-a9cf-29da7ad5852f', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-02 15:32:12.269376', '2026-03-02 15:32:16.516455', 4, 'completed', NULL, '2026-03-02 15:32:12.26958');
INSERT INTO public.workout_sessions VALUES ('09225e31-8d00-4015-893c-f4199df8ce16', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-02 16:18:30.618105', '2026-03-02 16:18:46.750189', 16, 'completed', NULL, '2026-03-02 16:18:30.618464');
INSERT INTO public.workout_sessions VALUES ('26850dc3-515d-40e3-9187-4cc4e408bf57', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-02 16:18:49.182923', '2026-03-02 16:18:59.159592', 10, 'completed', NULL, '2026-03-02 16:18:49.183146');
INSERT INTO public.workout_sessions VALUES ('5fc33dba-a069-4aef-9f0c-f4d33a0621d3', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-02 16:19:00.762915', '2026-03-02 16:19:08.244345', 7, 'completed', NULL, '2026-03-02 16:19:00.763159');
INSERT INTO public.workout_sessions VALUES ('38d2f160-b2b9-41eb-911d-c014320c4934', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-02 16:52:52.751766', '2026-03-02 16:53:08.42419', 16, 'completed', NULL, '2026-03-02 16:52:52.752024');
INSERT INTO public.workout_sessions VALUES ('4ccfc532-2b92-43a2-be5f-0734d1d15dfc', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-02 16:53:10.425858', '2026-03-02 16:53:26.122506', 16, 'completed', NULL, '2026-03-02 16:53:10.426056');
INSERT INTO public.workout_sessions VALUES ('49bc6c28-ad3e-4c4d-a37f-dc74cc7142d8', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-02 16:53:39.604517', '2026-03-02 16:53:56.578583', 17, 'completed', NULL, '2026-03-02 16:53:39.604697');
INSERT INTO public.workout_sessions VALUES ('5c57ea64-8a45-41e2-b17d-6bb6df748507', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-02 16:54:10.539241', '2026-03-02 16:54:28.5214', 18, 'completed', NULL, '2026-03-02 16:54:10.540194');
INSERT INTO public.workout_sessions VALUES ('44a1c133-1bb6-4499-b621-f75dd8b09e3d', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-02 16:55:04.36413', '2026-03-02 16:55:08.246191', 4, 'completed', NULL, '2026-03-02 16:55:04.364393');
INSERT INTO public.workout_sessions VALUES ('de40fa73-891a-4c64-963a-ecccfe127a58', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-02 16:56:59.321924', '2026-03-02 16:57:17.760558', 18, 'completed', NULL, '2026-03-02 16:56:59.322217');
INSERT INTO public.workout_sessions VALUES ('4554c698-72f4-4bc3-9942-471bf12445fa', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-02 16:57:21.652777', '2026-03-02 16:57:50.600004', 29, 'completed', NULL, '2026-03-02 16:57:21.655906');
INSERT INTO public.workout_sessions VALUES ('6e5a0dc7-bd40-47b7-8022-16e83375fd98', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-02 16:57:52.768101', '2026-03-02 16:58:04.924485', 12, 'completed', NULL, '2026-03-02 16:57:52.773951');
INSERT INTO public.workout_sessions VALUES ('349f48c0-2467-4282-88e9-e9deccbb6609', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 12:52:08.554703', '2026-03-03 12:52:26.370981', 18, 'completed', NULL, '2026-03-03 12:52:08.563268');
INSERT INTO public.workout_sessions VALUES ('7048c981-b3f6-4dc8-86f1-70d5f8b1c833', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 12:52:28.41867', '2026-03-03 12:53:00.573526', 32, 'completed', NULL, '2026-03-03 12:52:28.419068');
INSERT INTO public.workout_sessions VALUES ('1da94cd3-54a4-4d4c-bef0-f4c7c1544dbe', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 12:53:04.463209', '2026-03-03 12:53:49.013346', 45, 'completed', NULL, '2026-03-03 12:53:04.463501');
INSERT INTO public.workout_sessions VALUES ('b08feaa4-38b6-4165-aa61-c8610176cc3b', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 12:53:52.187971', '2026-03-03 12:53:59.454764', 7, 'completed', NULL, '2026-03-03 12:53:52.188287');
INSERT INTO public.workout_sessions VALUES ('bbf0a899-be97-4d96-9966-ec751f53634a', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 12:54:07.137863', '2026-03-03 12:54:34.582444', 27, 'completed', NULL, '2026-03-03 12:54:07.138123');
INSERT INTO public.workout_sessions VALUES ('262de3e1-6d83-4b77-a2cf-c82af4e5c913', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 12:54:56.987988', NULL, NULL, 'in_progress', NULL, '2026-03-03 12:54:56.988169');
INSERT INTO public.workout_sessions VALUES ('e07bbeb7-e9ea-4bf3-a829-a03b12948206', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 12:55:01.532081', NULL, NULL, 'in_progress', NULL, '2026-03-03 12:55:01.532264');
INSERT INTO public.workout_sessions VALUES ('b66a71ef-d63c-4dcb-9d17-80a4977de730', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 13:29:50.810364', '2026-03-03 13:30:02.058503', 11, 'completed', NULL, '2026-03-03 13:29:50.810652');
INSERT INTO public.workout_sessions VALUES ('2e4a9529-82b1-4df7-9b76-14f5e052e72a', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 13:13:38.050781', '2026-03-03 13:13:42.552918', 5, 'completed', NULL, '2026-03-03 13:13:38.051202');
INSERT INTO public.workout_sessions VALUES ('26d0ceac-13f8-4c60-8859-a2f6e0d3c65b', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 13:30:10.499202', '2026-03-03 13:30:13.876754', 3, 'completed', NULL, '2026-03-03 13:30:10.49957');
INSERT INTO public.workout_sessions VALUES ('70884907-6b48-4185-b600-a8557e3d6bd4', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 13:30:17.352395', '2026-03-03 13:30:24.118727', 7, 'completed', NULL, '2026-03-03 13:30:17.352777');
INSERT INTO public.workout_sessions VALUES ('971bb059-6a98-41e4-96df-359ebc16dac2', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 13:32:20.897462', NULL, NULL, 'in_progress', NULL, '2026-03-03 13:32:20.897637');
INSERT INTO public.workout_sessions VALUES ('ff001318-f0ff-4178-93a0-b99fe2be847b', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 13:32:24.446114', NULL, NULL, 'in_progress', NULL, '2026-03-03 13:32:24.446309');
INSERT INTO public.workout_sessions VALUES ('12b820a6-39b0-4963-bf8f-cc9d835cd8a5', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 13:38:31.338237', '2026-03-03 13:39:12.250745', 41, 'completed', NULL, '2026-03-03 13:38:31.338532');
INSERT INTO public.workout_sessions VALUES ('6cb1d384-d005-4357-a869-0a719452b56d', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 15:11:22.01178', '2026-03-03 15:11:34.834588', 13, 'completed', NULL, '2026-03-03 15:11:22.012109');
INSERT INTO public.workout_sessions VALUES ('3778ea62-f075-4060-896d-9cbd911397c3', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 15:12:17.00111', '2026-03-03 15:12:34.326379', 17, 'completed', NULL, '2026-03-03 15:12:17.00136');
INSERT INTO public.workout_sessions VALUES ('0e1ee44d-514c-4df9-81f5-76b190c74a66', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 15:12:39.022933', '2026-03-03 15:13:01.64353', 23, 'completed', NULL, '2026-03-03 15:12:39.023211');
INSERT INTO public.workout_sessions VALUES ('1575ad4a-c063-49e5-96c9-a3ef16688cab', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 15:13:04.61737', '2026-03-03 15:13:10.072386', 5, 'completed', NULL, '2026-03-03 15:13:04.617534');
INSERT INTO public.workout_sessions VALUES ('52b7ec0a-cfc2-49df-93a1-ee5ef577de54', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 15:13:36.591899', '2026-03-03 15:13:53.289778', 17, 'completed', NULL, '2026-03-03 15:13:36.592193');
INSERT INTO public.workout_sessions VALUES ('10ecf982-77c4-4d60-a681-7c0cce07635c', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 15:16:55.659931', '2026-03-03 15:17:08.385691', 13, 'completed', NULL, '2026-03-03 15:16:55.660217');
INSERT INTO public.workout_sessions VALUES ('fc04bc1d-8d15-4c68-aba3-f9e155d899d3', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 15:17:13.406117', '2026-03-03 15:17:23.351', 10, 'completed', NULL, '2026-03-03 15:17:13.40637');
INSERT INTO public.workout_sessions VALUES ('0ba5545e-53ef-4a1a-b2df-3d937479672e', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 15:17:28.407242', NULL, NULL, 'in_progress', NULL, '2026-03-03 15:17:28.407438');
INSERT INTO public.workout_sessions VALUES ('f2bc6a44-2c31-48f1-8514-a9ddb9470d4e', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 15:17:51.136131', '2026-03-03 15:18:08.608812', 17, 'completed', NULL, '2026-03-03 15:17:51.136366');
INSERT INTO public.workout_sessions VALUES ('38c0138b-4a1e-4c1b-8e41-456273348610', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 15:29:02.479197', '2026-03-03 15:29:16.218326', 14, 'completed', NULL, '2026-03-03 15:29:02.479536');
INSERT INTO public.workout_sessions VALUES ('ed390e98-62fb-4262-962f-3f0fd5684101', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 15:29:19.328827', '2026-03-03 15:29:51.938716', 33, 'completed', NULL, '2026-03-03 15:29:19.32922');
INSERT INTO public.workout_sessions VALUES ('d05f4060-85f5-44cb-a3d9-d7cc572a74f8', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 15:29:56.652443', '2026-03-03 15:30:02.534916', 6, 'completed', NULL, '2026-03-03 15:29:56.652776');
INSERT INTO public.workout_sessions VALUES ('94ac0446-5e4b-40bc-9833-4d74d8d758d7', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 15:38:53.516993', '2026-03-03 15:39:20.338214', 27, 'completed', NULL, '2026-03-03 15:38:53.517497');
INSERT INTO public.workout_sessions VALUES ('35147f9b-045d-48a2-811f-03cbdcd58f76', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 15:44:48.724426', '2026-03-03 15:45:05.650045', 17, 'completed', NULL, '2026-03-03 15:44:48.7249');
INSERT INTO public.workout_sessions VALUES ('191382e7-a1ca-418d-964a-9877b357d476', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 15:46:06.031715', '2026-03-03 15:46:07.258449', 1, 'completed', NULL, '2026-03-03 15:46:06.032118');
INSERT INTO public.workout_sessions VALUES ('8980ce8e-e642-4033-a3fe-3f7c63185966', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 15:46:21.698014', '2026-03-03 15:46:23.133596', 1, 'completed', NULL, '2026-03-03 15:46:21.698387');
INSERT INTO public.workout_sessions VALUES ('099f5ae6-afaf-45e6-b573-fdff50eb605a', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 15:52:07.307649', '2026-03-03 15:52:25.978716', 19, 'completed', NULL, '2026-03-03 15:52:07.308013');
INSERT INTO public.workout_sessions VALUES ('6fd48b29-c2e7-4c39-8149-ee4118cbae88', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 15:55:34.11168', '2026-03-03 15:55:45.600477', 11, 'completed', NULL, '2026-03-03 15:55:34.112124');
INSERT INTO public.workout_sessions VALUES ('eb0ce2d7-04b0-4377-981f-00c7cbcae1fd', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 15:55:53.506668', NULL, NULL, 'in_progress', NULL, '2026-03-03 15:55:53.506982');
INSERT INTO public.workout_sessions VALUES ('ffee41f4-da29-4861-8b8a-f1b1df77ec01', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 15:56:21.00973', NULL, NULL, 'in_progress', NULL, '2026-03-03 15:56:21.010265');
INSERT INTO public.workout_sessions VALUES ('11def149-5dfa-4cd2-9997-d539bcb1bca8', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 15:56:54.079655', '2026-03-03 15:57:14.34039', 20, 'completed', NULL, '2026-03-03 15:56:54.080147');
INSERT INTO public.workout_sessions VALUES ('44067d08-6c23-4cc5-b41e-5b2bceb597c6', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 15:57:54.269172', NULL, NULL, 'in_progress', NULL, '2026-03-03 15:57:54.269386');
INSERT INTO public.workout_sessions VALUES ('20bbea31-013e-4426-b8ae-c6073ce1232e', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 16:52:51.135088', NULL, NULL, 'in_progress', NULL, '2026-03-03 16:52:51.135779');
INSERT INTO public.workout_sessions VALUES ('30d61965-7a9d-4f13-b015-67f3251d7394', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 17:03:02.664185', '2026-03-03 17:03:24.688429', 22, 'completed', NULL, '2026-03-03 17:03:02.66478');
INSERT INTO public.workout_sessions VALUES ('3627a963-82ac-4e82-9cdc-d9ea1e479938', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 17:09:07.268604', '2026-03-03 17:09:33.262085', 26, 'completed', NULL, '2026-03-03 17:09:07.269302');
INSERT INTO public.workout_sessions VALUES ('71538e65-c12b-4aa1-a284-f69fe0bbd559', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 17:13:43.065441', '2026-03-03 17:14:02.009628', 19, 'completed', NULL, '2026-03-03 17:13:43.066415');
INSERT INTO public.workout_sessions VALUES ('27a19bf5-d5c2-45e7-a5fb-77133d6911be', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 17:14:11.835363', NULL, NULL, 'in_progress', NULL, '2026-03-03 17:14:11.836053');
INSERT INTO public.workout_sessions VALUES ('50e99864-211b-4bd1-9252-0171b781a7d3', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 17:15:01.507544', '2026-03-03 17:15:36.742387', 35, 'completed', NULL, '2026-03-03 17:15:01.508212');
INSERT INTO public.workout_sessions VALUES ('d596d990-426d-4b53-b82e-01adc55d9f88', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 17:20:25.098615', '2026-03-03 17:22:12.511755', 107, 'completed', NULL, '2026-03-03 17:20:25.099303');
INSERT INTO public.workout_sessions VALUES ('cd43114e-bf1b-44d1-845a-0dada626b6c3', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 17:22:14.662252', '2026-03-03 17:22:42.109327', 27, 'completed', NULL, '2026-03-03 17:22:14.662917');
INSERT INTO public.workout_sessions VALUES ('262ef773-4d26-4968-ba88-77e401fcc2d8', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 17:22:47.123323', '2026-03-03 17:23:29.561671', 42, 'completed', NULL, '2026-03-03 17:22:47.124066');
INSERT INTO public.workout_sessions VALUES ('7037d492-6475-46d1-9005-ef105c7b4cef', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 17:24:07.407785', NULL, NULL, 'in_progress', NULL, '2026-03-03 17:24:07.408517');
INSERT INTO public.workout_sessions VALUES ('6f4029c6-4a3b-4526-82e3-60ab376275c4', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 17:26:53.092306', '2026-03-03 17:27:00.361686', 7, 'completed', NULL, '2026-03-03 17:26:53.093047');
INSERT INTO public.workout_sessions VALUES ('af9dee5c-3d27-4c24-a6a8-6495c479c897', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 17:27:02.923152', NULL, NULL, 'in_progress', NULL, '2026-03-03 17:27:02.92338');
INSERT INTO public.workout_sessions VALUES ('0cdf5a5c-033b-4b5b-bd0f-46c637f85036', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 17:27:36.098279', '2026-03-03 17:30:04.887007', 149, 'completed', NULL, '2026-03-03 17:27:36.098865');
INSERT INTO public.workout_sessions VALUES ('63b4f710-f042-48fe-936d-7ec2440bd1ce', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 17:30:09.802546', '2026-03-03 17:32:20.5703', 131, 'completed', NULL, '2026-03-03 17:30:09.80316');
INSERT INTO public.workout_sessions VALUES ('c29f608c-adc6-4965-b19c-b28559c137ac', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 17:49:12.805031', NULL, NULL, 'in_progress', NULL, '2026-03-03 17:49:12.805756');
INSERT INTO public.workout_sessions VALUES ('71388c1d-e0f8-401d-b5d6-acd407598260', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 17:50:05.845234', NULL, NULL, 'in_progress', NULL, '2026-03-03 17:50:05.845882');
INSERT INTO public.workout_sessions VALUES ('4e839bbe-6a27-4db4-b89b-3e05aac13393', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 17:50:58.63726', NULL, NULL, 'in_progress', NULL, '2026-03-03 17:50:58.638056');
INSERT INTO public.workout_sessions VALUES ('9ee08690-d161-49c0-af8f-cbc244fabb57', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 17:59:06.729453', '2026-03-03 18:01:39.408839', 153, 'completed', NULL, '2026-03-03 17:59:06.730111');
INSERT INTO public.workout_sessions VALUES ('e5353692-4b6c-4d16-9e84-a6e1c4ff9c87', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 18:01:43.506172', '2026-03-03 18:01:52.311793', 9, 'completed', NULL, '2026-03-03 18:01:43.506899');
INSERT INTO public.workout_sessions VALUES ('9e81fc43-4815-4b63-b48c-d849717fa680', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 18:27:06.727428', NULL, NULL, 'in_progress', NULL, '2026-03-03 18:27:06.728044');
INSERT INTO public.workout_sessions VALUES ('fa402ade-337c-4285-b5df-c4fb50c1c45e', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 18:29:09.609271', '2026-03-03 18:30:02.238189', 53, 'completed', NULL, '2026-03-03 18:29:09.609868');
INSERT INTO public.workout_sessions VALUES ('d19e885e-6b6e-4d90-ae4f-a06e8a091443', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 18:30:40.02016', '2026-03-03 18:32:16.262313', 96, 'completed', NULL, '2026-03-03 18:30:40.020742');
INSERT INTO public.workout_sessions VALUES ('d2e586da-c046-4df1-ac76-c3f31dba3d46', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 18:35:37.803542', NULL, NULL, 'in_progress', NULL, '2026-03-03 18:35:37.804152');
INSERT INTO public.workout_sessions VALUES ('22bc7f23-b422-4cd2-bc43-240cc7592499', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 18:35:58.283349', NULL, NULL, 'in_progress', NULL, '2026-03-03 18:35:58.283668');
INSERT INTO public.workout_sessions VALUES ('4e5b458e-d308-42f8-8d92-3be8d3c90576', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 20:28:06.944396', '2026-03-03 20:29:33.848542', 87, 'completed', NULL, '2026-03-03 20:28:06.945106');
INSERT INTO public.workout_sessions VALUES ('122b2381-1726-4725-b9b5-9e2efcb8f645', '643b6d41-9df6-4e48-ac9b-12eb5e714b85', '2026-03-03 20:35:42.736363', NULL, NULL, 'in_progress', NULL, '2026-03-03 20:35:42.737185');
INSERT INTO public.workout_sessions VALUES ('8ce40792-7bbc-4e08-8ed0-e42a537d3755', '643b6d41-9df6-4e48-ac9b-12eb5e714b85', '2026-03-03 20:35:56.32428', NULL, NULL, 'in_progress', NULL, '2026-03-03 20:35:56.324991');
INSERT INTO public.workout_sessions VALUES ('7515fee8-42c7-4693-bad7-6b136dea22cb', '643b6d41-9df6-4e48-ac9b-12eb5e714b85', '2026-03-03 20:36:18.990456', NULL, NULL, 'in_progress', NULL, '2026-03-03 20:36:18.991108');
INSERT INTO public.workout_sessions VALUES ('750d321f-9e01-45bc-9b46-225bfa8790e2', '643b6d41-9df6-4e48-ac9b-12eb5e714b85', '2026-03-03 20:36:24.410983', NULL, NULL, 'in_progress', NULL, '2026-03-03 20:36:24.411802');
INSERT INTO public.workout_sessions VALUES ('a05eb591-f6de-4a07-9804-fced2147b0ac', '643b6d41-9df6-4e48-ac9b-12eb5e714b85', '2026-03-03 20:36:34.861098', NULL, NULL, 'in_progress', NULL, '2026-03-03 20:36:34.861726');
INSERT INTO public.workout_sessions VALUES ('9acd782e-5c7e-4cd9-913c-8104a94adb37', '643b6d41-9df6-4e48-ac9b-12eb5e714b85', '2026-03-03 20:38:17.773336', NULL, NULL, 'in_progress', NULL, '2026-03-03 20:38:17.77391');
INSERT INTO public.workout_sessions VALUES ('80f668c1-0b9c-4050-9824-04b196ab0200', '643b6d41-9df6-4e48-ac9b-12eb5e714b85', '2026-03-03 20:39:23.105954', '2026-03-03 20:40:17.081767', 54, 'completed', NULL, '2026-03-03 20:39:23.106571');
INSERT INTO public.workout_sessions VALUES ('305412b0-9b53-4f1e-87b0-2c77f17d83b1', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 21:58:35.861121', '2026-03-03 21:58:40.026819', 4, 'completed', NULL, '2026-03-03 21:58:35.862429');
INSERT INTO public.workout_sessions VALUES ('7f8ffa2e-0893-4348-a9b6-3dd59e759eb1', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 21:58:44.567163', '2026-03-03 21:58:53.950529', 9, 'completed', NULL, '2026-03-03 21:58:44.56787');
INSERT INTO public.workout_sessions VALUES ('d17e6aa6-3639-449e-a57b-97ca5e7fe203', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-03 21:58:59.824024', NULL, NULL, 'in_progress', NULL, '2026-03-03 21:58:59.824713');
INSERT INTO public.workout_sessions VALUES ('944848be-fee3-4e25-8716-480e4ee838aa', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-04 16:52:33.901254', NULL, NULL, 'in_progress', NULL, '2026-03-04 16:52:33.909571');
INSERT INTO public.workout_sessions VALUES ('52c87b96-bbd1-4ee2-82c4-0627ccced695', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-04 17:04:16.643936', NULL, NULL, 'in_progress', NULL, '2026-03-04 17:04:16.644176');
INSERT INTO public.workout_sessions VALUES ('bb124e75-7cf8-4901-9a47-1152b4763df0', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-04 17:09:05.677511', NULL, NULL, 'in_progress', NULL, '2026-03-04 17:09:05.677752');
INSERT INTO public.workout_sessions VALUES ('ed0529aa-03df-47ee-a338-1ce1c2d25ae3', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-04 17:14:58.700244', NULL, NULL, 'in_progress', NULL, '2026-03-04 17:14:58.70055');
INSERT INTO public.workout_sessions VALUES ('569761bc-0b56-4d96-9e1b-8c9a56b46423', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-04 17:17:02.783298', NULL, NULL, 'in_progress', NULL, '2026-03-04 17:17:02.783602');
INSERT INTO public.workout_sessions VALUES ('be27c380-68e1-44bf-a347-7172dd52a0c0', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-04 17:22:08.794876', NULL, NULL, 'in_progress', NULL, '2026-03-04 17:22:08.795166');
INSERT INTO public.workout_sessions VALUES ('26e1e0c8-2d76-41c4-91c4-f6a9492916b8', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-04 17:25:37.98438', NULL, NULL, 'in_progress', NULL, '2026-03-04 17:25:37.984653');
INSERT INTO public.workout_sessions VALUES ('a9950b65-633c-4b81-a2e9-e8bc9d4f9279', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-04 17:40:23.636644', '2026-03-04 17:41:23.954892', 60, 'completed', NULL, '2026-03-04 17:40:23.636999');
INSERT INTO public.workout_sessions VALUES ('f791689e-2860-429a-8103-80c8c3d33684', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-04 18:21:50.12735', NULL, NULL, 'in_progress', NULL, '2026-03-04 18:21:50.128028');
INSERT INTO public.workout_sessions VALUES ('ee575410-8b36-4b35-be9a-e05c2981ccac', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-04 18:22:07.128022', '2026-03-04 18:23:46.776966', 100, 'completed', NULL, '2026-03-04 18:22:07.128308');
INSERT INTO public.workout_sessions VALUES ('ab1a3130-7f22-4200-92f2-7581c95deba9', '643b6d41-9df6-4e48-ac9b-12eb5e714b85', '2026-03-06 13:13:27.052456', NULL, NULL, 'in_progress', NULL, '2026-03-06 13:13:27.060895');
INSERT INTO public.workout_sessions VALUES ('e2d92a54-72a2-428a-89cc-65fb126a7136', '643b6d41-9df6-4e48-ac9b-12eb5e714b85', '2026-03-06 13:13:33.708432', NULL, NULL, 'in_progress', NULL, '2026-03-06 13:13:33.708777');
INSERT INTO public.workout_sessions VALUES ('3c10dfb2-9f49-4109-867d-956a2633145d', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-06 14:20:00.767265', NULL, NULL, 'in_progress', NULL, '2026-03-06 14:20:00.775822');
INSERT INTO public.workout_sessions VALUES ('efaf8840-8c02-4b9b-908e-3c741bf2b689', '9b8d85a4-b6c2-4839-a70b-5f597496af02', '2026-03-06 14:20:29.016039', NULL, NULL, 'in_progress', NULL, '2026-03-06 14:20:29.016309');


--
-- Name: achievements_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.achievements_id_seq', 1, false);


--
-- Name: exercise_categories_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.exercise_categories_id_seq', 3, true);


--
-- Name: achievements achievements_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.achievements
    ADD CONSTRAINT achievements_pkey PRIMARY KEY (id);


--
-- Name: daily_stats daily_stats_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.daily_stats
    ADD CONSTRAINT daily_stats_pkey PRIMARY KEY (id);


--
-- Name: daily_stats daily_stats_user_id_stat_date_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.daily_stats
    ADD CONSTRAINT daily_stats_user_id_stat_date_key UNIQUE (user_id, stat_date);


--
-- Name: exercise_categories exercise_categories_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.exercise_categories
    ADD CONSTRAINT exercise_categories_name_key UNIQUE (name);


--
-- Name: exercise_categories exercise_categories_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.exercise_categories
    ADD CONSTRAINT exercise_categories_pkey PRIMARY KEY (id);


--
-- Name: exercise_sets exercise_sets_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.exercise_sets
    ADD CONSTRAINT exercise_sets_pkey PRIMARY KEY (id);


--
-- Name: exercise_stats exercise_stats_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.exercise_stats
    ADD CONSTRAINT exercise_stats_pkey PRIMARY KEY (id);


--
-- Name: exercise_stats exercise_stats_user_id_exercise_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.exercise_stats
    ADD CONSTRAINT exercise_stats_user_id_exercise_id_key UNIQUE (user_id, exercise_id);


--
-- Name: exercises exercises_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.exercises
    ADD CONSTRAINT exercises_pkey PRIMARY KEY (id);


--
-- Name: overall_stats overall_stats_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.overall_stats
    ADD CONSTRAINT overall_stats_pkey PRIMARY KEY (user_id);


--
-- Name: user_achievements user_achievements_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_achievements
    ADD CONSTRAINT user_achievements_pkey PRIMARY KEY (id);


--
-- Name: user_achievements user_achievements_user_id_achievement_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_achievements
    ADD CONSTRAINT user_achievements_user_id_achievement_id_key UNIQUE (user_id, achievement_id);


--
-- Name: user_exercise_progress user_exercise_progress_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_exercise_progress
    ADD CONSTRAINT user_exercise_progress_pkey PRIMARY KEY (id);


--
-- Name: user_exercise_progress user_exercise_progress_user_id_exercise_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_exercise_progress
    ADD CONSTRAINT user_exercise_progress_user_id_exercise_id_key UNIQUE (user_id, exercise_id);


--
-- Name: user_settings user_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_settings
    ADD CONSTRAINT user_settings_pkey PRIMARY KEY (user_id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users users_username_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_key UNIQUE (username);


--
-- Name: workout_sessions workout_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workout_sessions
    ADD CONSTRAINT workout_sessions_pkey PRIMARY KEY (id);


--
-- Name: idx_daily_stats_date; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_daily_stats_date ON public.daily_stats USING btree (stat_date);


--
-- Name: idx_daily_stats_user; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_daily_stats_user ON public.daily_stats USING btree (user_id);


--
-- Name: idx_exercise_sets_exercise; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_exercise_sets_exercise ON public.exercise_sets USING btree (exercise_id);


--
-- Name: idx_exercise_sets_session; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_exercise_sets_session ON public.exercise_sets USING btree (session_id);


--
-- Name: idx_exercise_stats_exercise; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_exercise_stats_exercise ON public.exercise_stats USING btree (exercise_id);


--
-- Name: idx_exercise_stats_user; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_exercise_stats_user ON public.exercise_stats USING btree (user_id);


--
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_email ON public.users USING btree (email);


--
-- Name: idx_users_username; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_username ON public.users USING btree (username);


--
-- Name: idx_workout_sessions_date; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_workout_sessions_date ON public.workout_sessions USING btree (started_at);


--
-- Name: idx_workout_sessions_user; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_workout_sessions_user ON public.workout_sessions USING btree (user_id);


--
-- Name: daily_stats update_daily_stats_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_daily_stats_updated_at BEFORE UPDATE ON public.daily_stats FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: exercise_stats update_exercise_stats_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_exercise_stats_updated_at BEFORE UPDATE ON public.exercise_stats FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: overall_stats update_overall_stats_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_overall_stats_updated_at BEFORE UPDATE ON public.overall_stats FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: user_exercise_progress update_user_progress_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_user_progress_updated_at BEFORE UPDATE ON public.user_exercise_progress FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: user_settings update_user_settings_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_user_settings_updated_at BEFORE UPDATE ON public.user_settings FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: users update_users_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON public.users FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: daily_stats daily_stats_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.daily_stats
    ADD CONSTRAINT daily_stats_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: exercise_sets exercise_sets_exercise_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.exercise_sets
    ADD CONSTRAINT exercise_sets_exercise_id_fkey FOREIGN KEY (exercise_id) REFERENCES public.exercises(id) ON DELETE CASCADE;


--
-- Name: exercise_sets exercise_sets_session_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.exercise_sets
    ADD CONSTRAINT exercise_sets_session_id_fkey FOREIGN KEY (session_id) REFERENCES public.workout_sessions(id) ON DELETE CASCADE;


--
-- Name: exercise_stats exercise_stats_exercise_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.exercise_stats
    ADD CONSTRAINT exercise_stats_exercise_id_fkey FOREIGN KEY (exercise_id) REFERENCES public.exercises(id) ON DELETE CASCADE;


--
-- Name: exercise_stats exercise_stats_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.exercise_stats
    ADD CONSTRAINT exercise_stats_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: exercises exercises_category_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.exercises
    ADD CONSTRAINT exercises_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.exercise_categories(id) ON DELETE SET NULL;


--
-- Name: overall_stats overall_stats_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.overall_stats
    ADD CONSTRAINT overall_stats_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_achievements user_achievements_achievement_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_achievements
    ADD CONSTRAINT user_achievements_achievement_id_fkey FOREIGN KEY (achievement_id) REFERENCES public.achievements(id) ON DELETE CASCADE;


--
-- Name: user_achievements user_achievements_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_achievements
    ADD CONSTRAINT user_achievements_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_exercise_progress user_exercise_progress_exercise_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_exercise_progress
    ADD CONSTRAINT user_exercise_progress_exercise_id_fkey FOREIGN KEY (exercise_id) REFERENCES public.exercises(id) ON DELETE CASCADE;


--
-- Name: user_exercise_progress user_exercise_progress_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_exercise_progress
    ADD CONSTRAINT user_exercise_progress_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_settings user_settings_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_settings
    ADD CONSTRAINT user_settings_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: workout_sessions workout_sessions_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workout_sessions
    ADD CONSTRAINT workout_sessions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict z9UbsbOkQ9gdeH3eSZqAaP4vKQDItMCy5S1tAo9zLVx7A7Maf9irNUTHcHh2CHJ

