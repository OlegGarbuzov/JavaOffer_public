@chcp 65001 >nul

@echo off
echo Запуск обфускации JavaScript файлов...

REM Проверка наличия Node.js
where node >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo Ошибка: Node.js не найден. Пожалуйста, установите Node.js.
    exit /b 1
)

REM Проверка наличия npm
where npm >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo Ошибка: npm не найден. Пожалуйста, установите npm.
    exit /b 1
)

REM Установка зависимостей, если они еще не установлены
echo Установка зависимостей...
call npm install

REM Запуск скрипта обфускации
echo Запуск обфускации...
call node obfuscate-prod.js

if %ERRORLEVEL% neq 0 (
    echo Ошибка: Процесс обфускации завершился с ошибкой.
    exit /b 1
) else (
    echo Обфускация успешно завершена!
)

exit /b 0 