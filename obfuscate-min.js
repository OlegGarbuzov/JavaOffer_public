const fs = require('fs');
const path = require('path');
const {glob} = require('glob');
const JavaScriptObfuscator = require('javascript-obfuscator');
const {minify} = require('terser');

// Конфигурация
const config = {
    // Исходные файлы для обфускации
    sourceFiles: [
        'src/main/resources/public/js/ui/ui-header-sc.js',
        'src/main/resources/public/js/ui/ui-header-sm.js',
        'src/main/resources/public/js/ui/ui-header-im.js',
        'src/main/resources/public/js/ui/ui-header-ot.js',
        'src/main/resources/public/js/ui/ui-header-ot-c.js',
        'src/main/resources/public/js/exam.js',
        'src/main/resources/public/js/testing.js'
    ],

    // Файлы для легкой минификации (без обфускации)
    lightMinifyFiles: [
        'src/main/resources/public/js/admin/security-utils.js',
        'src/main/resources/public/js/admin/questions.js',
        'src/main/resources/public/js/admin/users.js',
        'src/main/resources/public/js/admin/feedback.js',
        'src/main/resources/public/js/theme-switcher.js',
        'src/main/resources/public/js/mode-select.js',
        'src/main/resources/public/js/radio-buttons-init.js',
        'src/main/resources/public/js/gifList.js',
        'src/main/resources/public/js/theme-force-apply.js',
        'src/main/resources/public/js/admin-burger.js',
        'src/main/resources/public/js/notification.js',
        'src/main/resources/public/js/burger.js',
        'src/main/resources/public/js/form-submit.js',
        'src/main/resources/public/js/mobile-menu.js',
        'src/main/resources/public/js/header.js',
        'src/main/resources/public/js/color-modes.js'
    ],

    // Опции обфускации
    obfuscationOptions: {
        // --- Абсолютно безопасные, почти не ломают ---
        compact: false, // Просто минификация — безопасно
        log: false, // Логирование — не влияет на код

        // --- Умеренно безопасные ---
        unicodeEscapeSequence: false, // Юникод-переводы — обычно безопасно, влияет только на читаемость
        disableConsoleOutput: false, // Отключение console.log — безопасно, но затрудняет отладку
        drop_debugger: false, // Удаление debugger — безопасно
        splitStrings: false, // Разделение строк — обычно безопасно, но иногда ломает динамические строки
        splitStringsChunkLength: 50, // Длина кусков строк (если включить splitStrings)

        // --- Начинаем быть осторожными ---
        simplify: false, // Упрощение выражений — может поменять логику при странных конструкциях
        numbersToExpressions: false, // Преобразование чисел — безопасно на 90%, но лучше тестировать

        // --- Уже надо внимательно проверять ---
        identifierNamesGenerator: 'hexadecimal', // Переименование переменных — безопасно при выключенном renameGlobals
        renameGlobals: false, // Переименование глобальных — ОПАСНО, если включить

        // Сохраняем имена функций, связанных с античитом
        reservedNames: [
            // Основные функции античита
            '_d8t',
            '_r5e',
            'checkDevTools',
            'reportDevToolsViolation',
            '_s4t',
            '_c7q',
            '_l4g',
            '_c8d',
            '_c5c',
            '_h5f',
            '_s9o',
            '_s7m',
            '_p7m',
            '_u8i',
            '_a7v',
            '_i9c',
            '_v7h',
            '_r0h',

            // Дополнительные функции и объекты античита
            '_0x2ef71b', // Деактивация
            '_0xcfc9b0', // Мониторинг
            '_0x322d62', // Heartbeat
            '_0x44f969', // Отправка событий
            '_0x44204d', // Отправка через XHR
            '_0x535b28', // Получение куки
            '_0x51a71a', // Мобильное обнаружение
            '_0x4fe573', // Остановка мониторинга
            '_0x5df17d', // Отображение модального окна

            // Объекты и методы UI меню
            'window._uiMenuSet',
            'isHeaderActive',
            'reportIntegrityViolation',
            'updateHeader',
            'checkIntegrity',
            'deactivateHeader',
            'initHeader',
            'getCurrentToken',
            'updateSecurityToken',
            'safeUpdateQuestionId',
            'isHeaderStopped',
            '_showTerminationModal',
            '_testDevTools',

            // Объекты DevTools и их свойства
            'window.__REACT_DEVTOOLS_GLOBAL_HOOK__',
            'window.__REDUX_DEVTOOLS_EXTENSION__',
            'window.__VUE_DEVTOOLS_GLOBAL_HOOK__',
            'window.Firebug',
            'window.eruda',
            'window.vconsole',
            'window.__VCONSOLE__',

            // Свойства и методы для проверки размеров окна
            'window.outerWidth',
            'window.innerWidth',
            'window.outerHeight',
            'window.innerHeight'
        ],

        stringArray: false, // Перемещение строк в массив — ломает, если строки используются динамически
        stringArrayEncoding: ['base64'], // Шифрование строк — безопасно только вместе с включенным stringArray
        stringArrayIndexShift: false, // Сдвиг индексов — безопасно на малых проектах
        stringArrayRotate: false, // Вращение массива строк — безопасно при простом доступе к строкам
        stringArrayShuffle: false, // Перемешивание — чуть рискованно, особенно с динамикой
        stringArrayCallsTransform: false, // Преобразование вызовов — усложняет разбор, может ломать сложные строки
        stringArrayWrappersCount: 0, // Кол-во обёрток — влияет на сложность, не на стабильность
        stringArrayWrappersType: 'variable', // Тип обёрток — более совместимый вариант

        transformObjectKeys: false, // Переименование ключей объектов — ломает доступ по строковым ключам

        // --- Уже зона риска ---
        controlFlowFlattening: false, // Сильно перемешивает поток — часто ломает асинхронный/рекурсивный код
        controlFlowFlatteningThreshold: 1,

        deadCodeInjection: false, // Вставка мертвого кода — создает мусор, иногда ломает оптимизацию
        deadCodeInjectionThreshold: 1,

        debugProtection: false, // Блокировка devtools — может вызывать зависания и баги
        debugProtectionInterval: 3,

        selfDefending: false // Самозащита — может ломать работу в некоторых окружениях
    },

    minifyOptions: {
        compress: {
            drop_console: false, // Безопасно — отключает консоль
            drop_debugger: false, // Удаляем debugger(точки останова) — безопасно
            passes: 4 // Число проходов минификатора — больше = агрессивнее, но иногда ломает
        },
        mangle: {
            toplevel: false, // !!!!!!! Ломает античит. Не включать. !! Переименование переменных верхнего уровня — включать осторожно
            properties: false, // !!!!!!! Ломает античит. Не включать. !! Переименование свойств объектов — зона риска
            reserved: [
                // Основные функции античита
                '_d8t',
                '_r5e',
                'checkDevTools',
                'reportDevToolsViolation',
                '_s4t',
                '_c7q',
                '_l4g',
                '_c8d',
                '_c5c',
                '_h5f',
                '_s9o',
                '_s7m',
                '_p7m',
                '_u8i',
                '_a7v',
                '_i9c',
                '_v7h',
                '_r0h',

                // Дополнительные функции и объекты античита
                '_0x2ef71b',
                '_0xcfc9b0',
                '_0x322d62',
                '_0x44f969',
                '_0x44204d',
                '_0x535b28',
                '_0x51a71a',
                '_0x4fe573',
                '_0x5df17d',

                // Объекты и методы UI меню
                'isHeaderActive',
                'reportIntegrityViolation',
                'updateHeader',
                'checkIntegrity',
                'deactivateHeader',
                'initHeader',
                'getCurrentToken',
                'updateSecurityToken',
                'safeUpdateQuestionId',
                'isHeaderStopped',
                '_showTerminationModal',
                '_testDevTools'
            ]
        },
        format: {
            comments: false // Просто убирает комментарии — безопасно
        }
    },

    // Опции для легкой минификации (без обфускации)
    lightMinifyOptions: {
        compress: {
            drop_console: false, // Оставляем console.log для минимального профиля
            drop_debugger: false, // Оставляем debugger
            passes: 1, // Минимальное количество проходов
            dead_code: true, // Удаляем мертвый код
            unused: false, // Не удаляем неиспользуемые переменные
            evaluate: true, // Вычисляем константные выражения
            join_vars: true, // Объединяем объявления переменных
            reduce_vars: false, // Не заменяем переменные константами
            warnings: false // Отключаем предупреждения
        },
        mangle: false, // Не переименовываем переменные
        format: {
            comments: false, // Удаляем комментарии
            beautify: false, // Не форматируем код
            indent_level: 0 // Минимальный отступ
        }
    }
};

// Функция для обфускации и минификации файла
async function obfuscateAndMinifyFile(filePath) {
    try {
        console.log(`Обработка файла: ${filePath}`);

        // Чтение исходного файла
        const sourceCode = fs.readFileSync(filePath, 'utf8');

        // Обфускация кода
        console.log(`Обфускация ${path.basename(filePath)}...`);
        const obfuscatedCode = JavaScriptObfuscator.obfuscate(
            sourceCode,
            config.obfuscationOptions
        ).getObfuscatedCode();

        // Минификация обфускированного кода
        console.log(`Минификация ${path.basename(filePath)}...`);
        const minifiedResult = await minify(obfuscatedCode, config.minifyOptions);

        if (!minifiedResult.code) {
            throw new Error(`Ошибка минификации для ${filePath}`);
        }

        // Проверка синтаксиса
        try {
            new Function(minifiedResult.code);
            console.log(`Синтаксис проверен для ${path.basename(filePath)}`);
        } catch (syntaxError) {
            console.error(`Ошибка синтаксиса в ${path.basename(filePath)}: ${syntaxError.message}`);
            // Запись оригинального обфускированного кода для отладки
            const debugPath = filePath.replace('.js', '.debug.js');
            fs.writeFileSync(debugPath, obfuscatedCode);
            console.log(`Записан отладочный файл: ${debugPath}`);
            throw syntaxError;
        }

        // Запись минифицированного файла
        const outputPath = filePath.replace('.js', '.min.js');
        fs.writeFileSync(outputPath, minifiedResult.code);
        console.log(`Записан минифицированный файл: ${outputPath}`);

        return true;
    } catch (error) {
        console.error(`Ошибка при обработке ${filePath}: ${error.message}`);
        return false;
    }
}

// Функция для легкой минификации файла
async function lightMinifyFile(filePath) {
    try {
        console.log(`Легкая минификация файла: ${filePath}`);

        // Чтение исходного файла
        const sourceCode = fs.readFileSync(filePath, 'utf8');

        // Минификация без обфускации
        console.log(`Минификация ${path.basename(filePath)}...`);
        const minifiedResult = await minify(sourceCode, config.lightMinifyOptions);

        if (!minifiedResult.code) {
            throw new Error(`Ошибка минификации для ${filePath}`);
        }

        // Проверка синтаксиса
        try {
            new Function(minifiedResult.code);
            console.log(`Синтаксис проверен для ${path.basename(filePath)}`);
        } catch (syntaxError) {
            console.error(`Ошибка синтаксиса в ${path.basename(filePath)}: ${syntaxError.message}`);
            throw syntaxError;
        }

        // Запись минифицированного файла
        const outputPath = filePath.replace('.js', '.min.js');
        fs.writeFileSync(outputPath, minifiedResult.code);
        console.log(`Записан минифицированный файл: ${outputPath}`);

        return true;
    } catch (error) {
        console.error(`Ошибка при легкой минификации ${filePath}: ${error.message}`);
        return false;
    }
}

// Главная функция
async function main() {
    console.log('Начало процесса обфускации и минификации...');

    let successCount = 0;
    let failCount = 0;

    // Обработка файлов с полной обфускацией
    console.log('\n=== Обфускация и минификация критичных файлов ===');
    for (const filePattern of config.sourceFiles) {
        const files = await glob(filePattern);

        for (const file of files) {
            const success = await obfuscateAndMinifyFile(file);
            if (success) {
                successCount++;
            } else {
                failCount++;
            }
        }
    }

    // Обработка файлов с легкой минификацией
    console.log('\n=== Легкая минификация остальных файлов ===');
    for (const filePattern of config.lightMinifyFiles) {
        const files = await glob(filePattern);

        for (const file of files) {
            const success = await lightMinifyFile(file);
            if (success) {
                successCount++;
            } else {
                failCount++;
            }
        }
    }

    console.log('\nРезультаты обработки:');
    console.log(`Успешно обработано: ${successCount} файлов`);
    console.log(`Ошибки при обработке: ${failCount} файлов`);

    if (failCount > 0) {
        process.exit(1);
    }
}

// Запуск скрипта
main().catch(error => {
    console.error('Критическая ошибка:', error);
    process.exit(1);
}); 