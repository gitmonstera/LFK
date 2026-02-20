# LFK

## Инструкция по запуску 

### Шаг 1 
  Создайте базу данных 
  
  Название БД - `lfkdg` (скрипт на созданеи базы данных в отдельном файле)

  После создания и заполнения базы данных измените поля в пакете `confid/config.go` на ваши данные.
  
### Шаг 2
  Откройте терминал, перейдите по следующему пути `/LFK/backend/cmd/`
  
  Затем запустить сервер Go `go run main.go`

### Шаг 3
  Откройте новый терминал, перейдите по следующему пути `backend/python_processor`
  
  Затем запустить модуль Python, для этого выполните следующие команды:

  ```python
    python3 -m venv venv
    source venv/bin/activate

    pip install -r requirements.txt

    python exercise_detector.py
  ```

### Шаг 4
  Запускаем тесты!

  В новом окне терминала переходим по пути `backend/python_processor/debug_frames`

  ```python
    source venv/bin/activate

    python test_client.py
  ```
