const fs = require('fs');
const path = require('path');
const {glob} = require('glob');
const CleanCSS = require('clean-css');

// Конфигурация
const config = {
    // Директории с CSS файлами
    cssDirectories: [
        'src/main/resources/public/css',
        'src/main/resources/public/css/admin'
    ],
    
    // Исключения (файлы, которые не нужно минифицировать)
    excludePatterns: [
        '*.min.css',           // Уже минифицированные файлы
        'bootstrap*.css',      // Bootstrap файлы
        '*.map'                // Source map файлы
    ],
    
    // Опции минификации CSS
    minifyOptions: {
        level: 2,              // Уровень оптимизации (0-2)
        format: 'keep-breaks', // Сохранять переносы строк для читаемости (можно убрать для максимального сжатия)
        compatibility: 'ie8',  // Совместимость с браузерами
        inline: ['local'],     // Встраивание локальных @import
        rebase: false          // Не изменять пути к файлам
    }
};

// Функция для проверки, нужно ли исключить файл
function shouldExcludeFile(filePath) {
    const fileName = path.basename(filePath);
    return config.excludePatterns.some(pattern => {
        if (pattern.includes('*')) {
            const regex = new RegExp(pattern.replace(/\*/g, '.*'));
            return regex.test(fileName);
        }
        return fileName === pattern;
    });
}

// Функция для минификации CSS файла
async function minifyCSSFile(filePath) {
    try {
        console.log(`Обработка файла: ${filePath}`);
        
        // Проверяем, нужно ли исключить файл
        if (shouldExcludeFile(filePath)) {
            console.log(`Пропускаем файл: ${path.basename(filePath)} (исключен)`);
            return;
        }
        
        // Чтение исходного файла
        const sourceCSS = fs.readFileSync(filePath, 'utf8');
        
        // Проверяем, есть ли уже минифицированная версия
        const parsedPath = path.parse(filePath);
        const minFilePath = path.join(parsedPath.dir, `${parsedPath.name}.min${parsedPath.ext}`);
        
        console.log(`Минификация ${path.basename(filePath)}...`);
        
        // Минификация CSS
        const minifyResult = new CleanCSS(config.minifyOptions).minify(sourceCSS);
        
        if (minifyResult.errors.length > 0) {
            console.error(`Ошибки при минификации ${filePath}:`);
            minifyResult.errors.forEach(error => console.error(`  - ${error}`));
            return;
        }
        
        if (minifyResult.warnings.length > 0) {
            console.warn(`Предупреждения при минификации ${filePath}:`);
            minifyResult.warnings.forEach(warning => console.warn(`  - ${warning}`));
        }
        
        // Запись минифицированного файла
        fs.writeFileSync(minFilePath, minifyResult.styles, 'utf8');
        
        // Статистика
        const originalSize = Buffer.byteLength(sourceCSS, 'utf8');
        const minifiedSize = Buffer.byteLength(minifyResult.styles, 'utf8');
        const compressionRatio = ((originalSize - minifiedSize) / originalSize * 100).toFixed(1);
        
        console.log(`✅ ${path.basename(filePath)} -> ${path.basename(minFilePath)}`);
        console.log(`   Размер: ${originalSize} bytes -> ${minifiedSize} bytes (сжатие: ${compressionRatio}%)`);
        
    } catch (error) {
        console.error(`❌ Ошибка при обработке файла ${filePath}:`, error.message);
    }
}

// Основная функция
async function main() {
    console.log('🎨 Запуск минификации CSS файлов...\n');
    
    try {
        let totalProcessed = 0;
        
        // Обработка каждой директории
        for (const directory of config.cssDirectories) {
            console.log(`📂 Обработка директории: ${directory}`);
            
            // Проверяем, существует ли директория
            if (!fs.existsSync(directory)) {
                console.warn(`⚠️  Директория не найдена: ${directory}`);
                continue;
            }
            
            // Поиск всех CSS файлов в директории
            const cssFiles = glob.sync(path.join(directory, '*.css').replace(/\\/g, '/'));
            
            if (cssFiles.length === 0) {
                console.log(`   Нет CSS файлов в директории: ${directory}`);
                continue;
            }
            
            console.log(`   Найдено CSS файлов: ${cssFiles.length}`);
            
            // Обработка каждого файла
            for (const cssFile of cssFiles) {
                await minifyCSSFile(cssFile);
                totalProcessed++;
            }
            
            console.log(''); // Пустая строка для разделения директорий
        }
        
        console.log(`🎉 Минификация завершена! Обработано файлов: ${totalProcessed}`);
        
    } catch (error) {
        console.error('❌ Критическая ошибка:', error.message);
        process.exit(1);
    }
}

// Запуск скрипта
if (require.main === module) {
    main();
} 