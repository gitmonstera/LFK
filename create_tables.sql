--
-- PostgreSQL database dump
--

\restrict GIC8c0bTvyigR5JlSSUWZkkGVQAv9e3NWpXc17zuphXtFxCYJ4usCIJlxocCR2X

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

\unrestrict GIC8c0bTvyigR5JlSSUWZkkGVQAv9e3NWpXc17zuphXtFxCYJ4usCIJlxocCR2X

