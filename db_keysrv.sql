--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

SET search_path = public, pg_catalog;

ALTER TABLE ONLY public.tokens DROP CONSTRAINT tokens_user_id_fkey;
ALTER TABLE ONLY public.tokens DROP CONSTRAINT tokens_service_id_fkey;
ALTER TABLE ONLY public.auth_infos DROP CONSTRAINT auth_infos_user_id_fkey;
ALTER TABLE ONLY public.auth_infos DROP CONSTRAINT auth_infos_token_id_fkey;
ALTER TABLE ONLY public.auth_infos DROP CONSTRAINT auth_infos_service_id_fkey;
ALTER TABLE ONLY public.users DROP CONSTRAINT users_pkey;
ALTER TABLE ONLY public.tokens DROP CONSTRAINT tokens_pkey;
ALTER TABLE ONLY public.services DROP CONSTRAINT services_pkey;
ALTER TABLE ONLY public.logs DROP CONSTRAINT log_pkey;
ALTER TABLE ONLY public.auth_infos DROP CONSTRAINT auth_infos_pkey;
ALTER TABLE public.users ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.tokens ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.services ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.logs ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.auth_infos ALTER COLUMN id DROP DEFAULT;
DROP SEQUENCE public.users_id_seq;
DROP TABLE public.users;
DROP SEQUENCE public.tokens_id_seq;
DROP TABLE public.tokens;
DROP SEQUENCE public.services_id_seq;
DROP TABLE public.services;
DROP SEQUENCE public.log_id_seq;
DROP TABLE public.logs;
DROP SEQUENCE public.auth_infos_id_seq;
DROP TABLE public.auth_infos;
DROP EXTENSION pgcrypto;
DROP EXTENSION plpgsql;
DROP SCHEMA public;
--
-- Name: public; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA public;


ALTER SCHEMA public OWNER TO postgres;

--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON SCHEMA public IS 'standard public schema';


--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: auth_infos; Type: TABLE; Schema: public; Owner: dbu_keysrv; Tablespace: 
--

CREATE TABLE auth_infos (
    id integer NOT NULL,
    user_id integer NOT NULL,
    service_id integer NOT NULL,
    bmu_authinfo_id integer NOT NULL,
    ai_key bytea,
    ai_value bytea,
    token_id integer
);


ALTER TABLE public.auth_infos OWNER TO dbu_keysrv;

--
-- Name: auth_infos_id_seq; Type: SEQUENCE; Schema: public; Owner: dbu_keysrv
--

CREATE SEQUENCE auth_infos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.auth_infos_id_seq OWNER TO dbu_keysrv;

--
-- Name: auth_infos_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dbu_keysrv
--

ALTER SEQUENCE auth_infos_id_seq OWNED BY auth_infos.id;


--
-- Name: logs; Type: TABLE; Schema: public; Owner: dbu_keysrv; Tablespace: 
--

CREATE TABLE logs (
    id integer NOT NULL,
    type bytea,
    description bytea,
    bmu_user_id integer NOT NULL,
    bmu_service_id integer,
    bmu_authinfo_id integer,
    bmu_token_id integer,
    date bigint
);


ALTER TABLE public.logs OWNER TO dbu_keysrv;

--
-- Name: log_id_seq; Type: SEQUENCE; Schema: public; Owner: dbu_keysrv
--

CREATE SEQUENCE log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.log_id_seq OWNER TO dbu_keysrv;

--
-- Name: log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dbu_keysrv
--

ALTER SEQUENCE log_id_seq OWNED BY logs.id;


--
-- Name: services; Type: TABLE; Schema: public; Owner: dbu_keysrv; Tablespace: 
--

CREATE TABLE services (
    id integer NOT NULL,
    bmu_service_id integer NOT NULL
);


ALTER TABLE public.services OWNER TO dbu_keysrv;

--
-- Name: services_id_seq; Type: SEQUENCE; Schema: public; Owner: dbu_keysrv
--

CREATE SEQUENCE services_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.services_id_seq OWNER TO dbu_keysrv;

--
-- Name: services_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dbu_keysrv
--

ALTER SEQUENCE services_id_seq OWNED BY services.id;


--
-- Name: tokens; Type: TABLE; Schema: public; Owner: dbu_keysrv; Tablespace: 
--

CREATE TABLE tokens (
    id integer NOT NULL,
    user_id integer NOT NULL,
    service_id integer NOT NULL,
    bmu_authinfo_id integer NOT NULL,
    token_key bytea,
    token_value bytea,
    token_id integer,
    backupdate bytea,
    reusable boolean NOT NULL
);


ALTER TABLE public.tokens OWNER TO dbu_keysrv;

--
-- Name: tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: dbu_keysrv
--

CREATE SEQUENCE tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.tokens_id_seq OWNER TO dbu_keysrv;

--
-- Name: tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dbu_keysrv
--

ALTER SEQUENCE tokens_id_seq OWNED BY tokens.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: dbu_keysrv; Tablespace: 
--

CREATE TABLE users (
    id integer NOT NULL,
    bmu_user_id integer NOT NULL,
    bmu_user_pwd_hash bytea
);


ALTER TABLE public.users OWNER TO dbu_keysrv;

--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: dbu_keysrv
--

CREATE SEQUENCE users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.users_id_seq OWNER TO dbu_keysrv;

--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dbu_keysrv
--

ALTER SEQUENCE users_id_seq OWNED BY users.id;


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dbu_keysrv
--

ALTER TABLE ONLY auth_infos ALTER COLUMN id SET DEFAULT nextval('auth_infos_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dbu_keysrv
--

ALTER TABLE ONLY logs ALTER COLUMN id SET DEFAULT nextval('log_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dbu_keysrv
--

ALTER TABLE ONLY services ALTER COLUMN id SET DEFAULT nextval('services_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dbu_keysrv
--

ALTER TABLE ONLY tokens ALTER COLUMN id SET DEFAULT nextval('tokens_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dbu_keysrv
--

ALTER TABLE ONLY users ALTER COLUMN id SET DEFAULT nextval('users_id_seq'::regclass);


--
-- Name: auth_infos_pkey; Type: CONSTRAINT; Schema: public; Owner: dbu_keysrv; Tablespace: 
--

ALTER TABLE ONLY auth_infos
    ADD CONSTRAINT auth_infos_pkey PRIMARY KEY (id);


--
-- Name: log_pkey; Type: CONSTRAINT; Schema: public; Owner: dbu_keysrv; Tablespace: 
--

ALTER TABLE ONLY logs
    ADD CONSTRAINT log_pkey PRIMARY KEY (id);


--
-- Name: services_pkey; Type: CONSTRAINT; Schema: public; Owner: dbu_keysrv; Tablespace: 
--

ALTER TABLE ONLY services
    ADD CONSTRAINT services_pkey PRIMARY KEY (id);


--
-- Name: tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: dbu_keysrv; Tablespace: 
--

ALTER TABLE ONLY tokens
    ADD CONSTRAINT tokens_pkey PRIMARY KEY (id);


--
-- Name: users_pkey; Type: CONSTRAINT; Schema: public; Owner: dbu_keysrv; Tablespace: 
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: auth_infos_service_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: dbu_keysrv
--

ALTER TABLE ONLY auth_infos
    ADD CONSTRAINT auth_infos_service_id_fkey FOREIGN KEY (service_id) REFERENCES services(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: auth_infos_token_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: dbu_keysrv
--

ALTER TABLE ONLY auth_infos
    ADD CONSTRAINT auth_infos_token_id_fkey FOREIGN KEY (token_id) REFERENCES tokens(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: auth_infos_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: dbu_keysrv
--

ALTER TABLE ONLY auth_infos
    ADD CONSTRAINT auth_infos_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: tokens_service_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: dbu_keysrv
--

ALTER TABLE ONLY tokens
    ADD CONSTRAINT tokens_service_id_fkey FOREIGN KEY (service_id) REFERENCES services(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: tokens_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: dbu_keysrv
--

ALTER TABLE ONLY tokens
    ADD CONSTRAINT tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

