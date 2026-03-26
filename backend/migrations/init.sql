--
-- Инициализация базы данных LFK
--

-- Создание расширения для UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Функция для автоматического обновления updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- Таблица пользователей
-- =====================================================
CREATE TABLE IF NOT EXISTS users (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    birth_date DATE,
    gender VARCHAR(10) CHECK (gender IN ('male', 'female', 'other')),
    height_cm INTEGER CHECK (height_cm > 0 AND height_cm < 300),
    weight_kg NUMERIC(5,2) CHECK (weight_kg > 0 AND weight_kg < 500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    role VARCHAR(20) DEFAULT 'user' CHECK (role IN ('user', 'admin', 'trainer'))
);

-- =====================================================
-- Таблица категорий упражнений
-- =====================================================
CREATE TABLE IF NOT EXISTS exercise_categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    icon VARCHAR(50),
    color VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- Таблица упражнений
-- =====================================================
CREATE TABLE IF NOT EXISTS exercises (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category_id INTEGER REFERENCES exercise_categories(id) ON DELETE SET NULL,
    difficulty_level INTEGER CHECK (difficulty_level >= 1 AND difficulty_level <= 5),
    target_muscles TEXT[],
    instructions TEXT[],
    duration_seconds INTEGER CHECK (duration_seconds > 0),
    calories_burn NUMERIC(5,2) CHECK (calories_burn >= 0),
    video_url VARCHAR(255),
    image_url VARCHAR(255),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

-- =====================================================
-- Таблица тренировок
-- =====================================================
CREATE TABLE IF NOT EXISTS workout_sessions (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP,
    duration_seconds INTEGER CHECK (duration_seconds >= 0),
    status VARCHAR(20) DEFAULT 'in_progress' CHECK (status IN ('in_progress', 'completed', 'abandoned')),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- Таблица подходов упражнений
-- =====================================================
CREATE TABLE IF NOT EXISTS exercise_sets (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    session_id UUID REFERENCES workout_sessions(id) ON DELETE CASCADE,
    exercise_id VARCHAR(50) REFERENCES exercises(id) ON DELETE CASCADE,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    target_repetitions INTEGER CHECK (target_repetitions >= 0),
    actual_repetitions INTEGER CHECK (actual_repetitions >= 0),
    target_duration_seconds INTEGER CHECK (target_duration_seconds >= 0),
    actual_duration_seconds INTEGER CHECK (actual_duration_seconds >= 0),
    accuracy_score NUMERIC(5,2) CHECK (accuracy_score >= 0 AND accuracy_score <= 100),
    completion_status VARCHAR(20) DEFAULT 'completed' CHECK (completion_status IN ('completed', 'failed', 'partial')),
    performance_data JSONB,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- Таблица дневной статистики
-- =====================================================
CREATE TABLE IF NOT EXISTS daily_stats (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    stat_date DATE NOT NULL,
    total_sessions INTEGER DEFAULT 0 CHECK (total_sessions >= 0),
    total_duration_seconds INTEGER DEFAULT 0 CHECK (total_duration_seconds >= 0),
    total_exercises INTEGER DEFAULT 0 CHECK (total_exercises >= 0),
    calories_burned NUMERIC(10,2) DEFAULT 0 CHECK (calories_burned >= 0),
    streak_day INTEGER DEFAULT 0 CHECK (streak_day >= 0),
    completed BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, stat_date)
);

-- =====================================================
-- Таблица статистики по упражнениям
-- =====================================================
CREATE TABLE IF NOT EXISTS exercise_stats (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    exercise_id VARCHAR(50) REFERENCES exercises(id) ON DELETE CASCADE,
    total_sessions INTEGER DEFAULT 0,
    total_repetitions INTEGER DEFAULT 0,
    total_duration INTEGER DEFAULT 0,
    best_accuracy NUMERIC(5,2) CHECK (best_accuracy >= 0 AND best_accuracy <= 100),
    avg_accuracy NUMERIC(5,2) CHECK (avg_accuracy >= 0 AND avg_accuracy <= 100),
    last_performed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, exercise_id)
);

-- =====================================================
-- Таблица общей статистики
-- =====================================================
CREATE TABLE IF NOT EXISTS overall_stats (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    total_sessions INTEGER DEFAULT 0,
    total_exercises INTEGER DEFAULT 0,
    total_repetitions INTEGER DEFAULT 0,
    total_duration INTEGER DEFAULT 0,
    unique_exercises INTEGER DEFAULT 0,
    current_streak INTEGER DEFAULT 0,
    longest_streak INTEGER DEFAULT 0,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_workout_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- Таблица достижений
-- =====================================================
CREATE TABLE IF NOT EXISTS achievements (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon VARCHAR(50),
    condition_type VARCHAR(50) CHECK (condition_type IN ('total_sessions', 'streak', 'accuracy', 'total_exercises')),
    condition_value INTEGER CHECK (condition_value > 0),
    experience_points INTEGER DEFAULT 10 CHECK (experience_points >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- Таблица достижений пользователей
-- =====================================================
CREATE TABLE IF NOT EXISTS user_achievements (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    achievement_id INTEGER REFERENCES achievements(id) ON DELETE CASCADE,
    earned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, achievement_id)
);

-- =====================================================
-- Таблица прогресса по упражнениям
-- =====================================================
CREATE TABLE IF NOT EXISTS user_exercise_progress (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    exercise_id VARCHAR(50) REFERENCES exercises(id) ON DELETE CASCADE,
    total_sessions INTEGER DEFAULT 0 CHECK (total_sessions >= 0),
    total_repetitions INTEGER DEFAULT 0 CHECK (total_repetitions >= 0),
    total_duration_seconds INTEGER DEFAULT 0 CHECK (total_duration_seconds >= 0),
    best_accuracy NUMERIC(5,2) CHECK (best_accuracy >= 0 AND best_accuracy <= 100),
    average_accuracy NUMERIC(5,2) CHECK (average_accuracy >= 0 AND average_accuracy <= 100),
    last_performed_at TIMESTAMP,
    current_streak INTEGER DEFAULT 0 CHECK (current_streak >= 0),
    max_streak INTEGER DEFAULT 0 CHECK (max_streak >= 0),
    level INTEGER DEFAULT 1 CHECK (level > 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, exercise_id)
);

-- =====================================================
-- Таблица настроек пользователей
-- =====================================================
CREATE TABLE IF NOT EXISTS user_settings (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    notification_enabled BOOLEAN DEFAULT true,
    reminder_time TIME DEFAULT '09:00:00',
    daily_goal_minutes INTEGER DEFAULT 30 CHECK (daily_goal_minutes >= 0),
    sound_enabled BOOLEAN DEFAULT true,
    voice_guide_enabled BOOLEAN DEFAULT true,
    theme VARCHAR(20) DEFAULT 'light' CHECK (theme IN ('light', 'dark', 'system')),
    language VARCHAR(10) DEFAULT 'ru' CHECK (language IN ('ru', 'en', 'uk')),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- Индексы для оптимизации запросов
-- =====================================================
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_workout_sessions_user ON workout_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_workout_sessions_date ON workout_sessions(started_at);
CREATE INDEX IF NOT EXISTS idx_exercise_sets_session ON exercise_sets(session_id);
CREATE INDEX IF NOT EXISTS idx_exercise_sets_exercise ON exercise_sets(exercise_id);
CREATE INDEX IF NOT EXISTS idx_daily_stats_user ON daily_stats(user_id);
CREATE INDEX IF NOT EXISTS idx_daily_stats_date ON daily_stats(stat_date);
CREATE INDEX IF NOT EXISTS idx_exercise_stats_user ON exercise_stats(user_id);
CREATE INDEX IF NOT EXISTS idx_exercise_stats_exercise ON exercise_stats(exercise_id);

-- =====================================================
-- Триггеры для автоматического обновления updated_at
-- =====================================================
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_daily_stats_updated_at BEFORE UPDATE ON daily_stats
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_exercise_stats_updated_at BEFORE UPDATE ON exercise_stats
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_overall_stats_updated_at BEFORE UPDATE ON overall_stats
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_progress_updated_at BEFORE UPDATE ON user_exercise_progress
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_settings_updated_at BEFORE UPDATE ON user_settings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- Начальные данные
-- =====================================================

-- Категории упражнений
INSERT INTO exercise_categories (name, description, icon, color) VALUES
    ('Руки', 'Упражнения для кистей и пальцев рук', '👐', '#FF6B6B'),
    ('Плечи', 'Упражнения для плечевого пояса', '💪', '#4ECDC4'),
    ('Спина', 'Упражнения для спины', '⬆️', '#45B7D1'),
    ('Моторика', 'Упражнения для развития мелкой моторики пальцев', '🤏', '#9B59B6')
ON CONFLICT (name) DO NOTHING;

-- Базовые упражнения
INSERT INTO exercises (id, name, description, category_id, difficulty_level, target_muscles, instructions, duration_seconds, calories_burn) VALUES
    ('fist', 'Кулак', 'Сжимайте и разжимайте кулаки для укрепления кистей', 
     (SELECT id FROM exercise_categories WHERE name = 'Руки'), 1, 
     ARRAY['Мышцы кисти', 'Сгибатели пальцев'],
     ARRAY['Сожмите кулак', 'Держите 3 секунды', 'Разожмите', 'Повторите 10 раз'],
     30, 2.5),
    
    ('fist-index', 'Кулак с указательным', 'Кулак с поднятым указательным пальцем для развития координации',
     (SELECT id FROM exercise_categories WHERE name = 'Руки'), 2,
     ARRAY['Мышцы кисти', 'Разгибатели пальцев'],
     ARRAY['Сожмите кулак', 'Поднимите указательный палец', 'Держите 3 секунды', 'Опустите палец'],
     30, 3.0),
    
    ('fist-palm', 'Кулак-ладонь', 'Чередование кулака и ладони для улучшения кровообращения',
     (SELECT id FROM exercise_categories WHERE name = 'Руки'), 2,
     ARRAY['Мышцы кисти', 'Мышцы предплечья'],
     ARRAY['Сожмите кулак', 'Держите 3 секунды', 'Раскройте ладонь', 'Держите 3 секунды'],
     60, 3.5),
    
    ('finger-touching', 'Считалочка', 'Поочередное касание пальцев - классическое упражнение для развития мелкой моторики и координации движений. По очереди соединяйте большой палец с указательным, средним, безымянным и мизинцем.',
     (SELECT id FROM exercise_categories WHERE name = 'Моторика'), 2,
     ARRAY['Мышцы кисти', 'Сгибатели пальцев', 'Мышцы-противопоставители'],
     ARRAY['Соедините большой палец с указательным', 'Верните в исходное положение', 'Соедините со средним', 'Повторите с безымянным', 'Завершите мизинцем', 'Повторите цикл 5-10 раз'],
     45, 2.5)
ON CONFLICT (id) DO NOTHING;

-- Добавление тестового пользователя (пароль: password123)
INSERT INTO users (username, email, password_hash, first_name, last_name, role) VALUES
    ('testuser', 'test@example.com', '$2a$10$nTlhjW4OzeTF0VJcw1J5/OtAR2evCBxKIqjwpWNZvhXlhN1Wxr676', 'Тест', 'Пользователь', 'user'),
    ('admin', 'admin@example.com', '$2a$10$nTlhjW4OzeTF0VJcw1J5/OtAR2evCBxKIqjwpWNZvhXlhN1Wxr676', 'Admin', 'User', 'admin')
ON CONFLICT (username) DO NOTHING;

-- Создание общей статистики для тестовых пользователей
INSERT INTO overall_stats (user_id)
    SELECT id FROM users WHERE username IN ('testuser', 'admin')
ON CONFLICT (user_id) DO NOTHING;

ALTER TABLE exercises
    ADD COLUMN IF NOT EXISTS applicable_codes TEXT[];

-- Комментарий к столбцу
COMMENT ON COLUMN exercises.applicable_codes IS 'Массив кодов МКБ-10 (или диапазонов), для которых показано упражнение. Например: {"S62.0–S62.9","M19.0","G81"}';

-- =====================================================
-- Обновление существующих упражнений
-- =====================================================

-- Упражнение "Кулак" (fist)
UPDATE exercises
SET applicable_codes = ARRAY[
    'S62.0–S62.9',          -- Переломы запястья, пясти, пальцев кисти
    'S63.0–S63.7',          -- Вывихи и повреждения связок лучезапястного сустава и кисти
    'M19.0–M19.9',          -- Артроз плечевого, локтевого, голеностопного и других суставов (в т.ч. кисти)
    'M65.0–M65.9',          -- Синовиты, теносиновиты
    'M79.0–M79.2',          -- Ревматизм мягких тканей, миалгии, невралгии
    'G81.0–G81.9',          -- Гемиплегия (постинсультная)
    'G60–G64',              -- Полинейропатии
    'Z50.0'                 -- Лечебная физкультура (основной код назначения)
    ]
WHERE id = 'fist';

-- Упражнение "Кулак с указательным" (fist-index)
UPDATE exercises
SET applicable_codes = ARRAY[
    'S62.0–S62.9',
    'S63.0–S63.7',
    'M19.0–M19.9',
    'M65.0–M65.9',
    'G81.0–G81.9',
    'G60–G64',
    'Z50.0'
    ]
WHERE id = 'fist-index';

-- Упражнение "Кулак-ладонь" (fist-palm)
UPDATE exercises
SET applicable_codes = ARRAY[
    'S62.0–S62.9',
    'S63.0–S63.7',
    'M19.0–M19.9',
    'M65.0–M65.9',
    'G81.0–G81.9',
    'G60–G64',
    'Z50.0'
    ]
WHERE id = 'fist-palm';

-- Упражнение "Считалочка" (finger-touching)
UPDATE exercises
SET applicable_codes = ARRAY[
    'S62.0–S62.9',
    'S63.0–S63.7',
    'M19.0–M19.9',
    'G81.0–G81.9',
    'G60–G64',
    'G35',                  -- Рассеянный склероз
    'I69.0–I69.4',          -- Последствия инсульта
    'Z50.0'
    ]
WHERE id = 'finger-touching';

-- =====================================================
-- Пример добавления нового упражнения с показаниями
-- =====================================================
-- INSERT INTO exercises (id, name, description, category_id, difficulty_level, target_muscles, instructions, duration_seconds, calories_burn, applicable_codes)
-- VALUES (
--     'wrist-rotation',
--     'Вращение кистью',
--     'Круговые движения кистью для улучшения подвижности лучезапястного сустава',
--     (SELECT id FROM exercise_categories WHERE name = 'Руки'),
--     1,
--     ARRAY['Мышцы предплечья', 'Лучезапястный сустав'],
--     ARRAY['Вытяните руку', 'Медленно вращайте кистью по часовой стрелке 10 раз', 'Повторите против часовой стрелки'],
--     30,
--     2.0,
--     ARRAY['S62.0–S62.9', 'S63.0–S63.7', 'M19.0', 'M70.0–M70.9', 'Z50.0']
-- );

-- =====================================================
-- Индекс для ускорения поиска по показаниям
-- =====================================================
CREATE INDEX IF NOT EXISTS idx_exercises_applicable_codes ON exercises USING gin (applicable_codes);