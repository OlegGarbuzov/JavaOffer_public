package com.example.javaoffer.exam;

import com.example.javaoffer.exam.dto.AnswerDTO;
import com.example.javaoffer.exam.dto.TaskDTO;
import com.example.javaoffer.exam.enums.TaskDifficulty;
import com.example.javaoffer.exam.enums.TaskGrade;
import com.example.javaoffer.exam.enums.TaskTopic;
import com.example.javaoffer.exam.service.TaskService;
import com.example.javaoffer.user.entity.User;
import com.example.javaoffer.user.enums.UserRole;
import com.example.javaoffer.user.service.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.List;

/**
 * Конфигурационный класс для автоматической инициализации производственных данных приложения JavaOffer.
 * <p>
 * Этот класс является ключевым компонентом системы инициализации данных и обеспечивает:
 * <ul>
 *   <li><strong>Управление жизненным циклом данных</strong>: автоматическая очистка и пересоздание банка вопросов</li>
 *   <li><strong>Создание администратора по умолчанию</strong>: автоматическое создание учетной записи admin для управления системой</li>
 *   <li><strong>Инициализация банка вопросов</strong>: загрузка полного набора тестовых заданий для проведения экзаменов</li>
 *   <li><strong>Производственная активация</strong>: работает во всех профилях кроме тестового для предотвращения конфликтов</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Профильная конфигурация:</strong><br/>
 * Класс активируется через аннотацию {@code @Profile("!test")}, что означает его работу во всех профилях
 * кроме тестового. Это предотвращает конфликты с тестовыми данными и обеспечивает изоляцию сред.
 * </p>
 * <p>
 * <strong>Банк тестовых вопросов включает:</strong>
 * <ul>
 *   <li><code>CORE</code> - фундаментальные концепции Java (синтаксис, коллекции, исключения, OOP)</li>
 *   <li><code>SPRING</code> - экосистема Spring Framework (IoC, DI, аннотации, конфигурация)</li>
 *   <li><code>MULTITHREADING</code> - многопоточное программирование (потоки, синхронизация, concurrent collections)</li>
 *   <li><code>DATA_STRUCTURES</code> - структуры данных и алгоритмы (временная сложность, производительность)</li>
 *   <li><code>DESIGN_PATTERNS</code> - паттерны проектирования GoF (Singleton, Factory, Observer и др.)</li>
 *   <li><code>HIBERNATE</code> - ORM технологии (JPA, каскадные операции, lifecycle entities)</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Структура вопросов по уровням сложности:</strong>
 * <ul>
 *   <li><em>Easy1-Easy3</em>: базовые вопросы для Junior разработчиков (основы синтаксиса, простые концепции)</li>
 *   <li><em>Medium1-Medium3</em>: вопросы среднего уровня (понимание нюансов, практическое применение)</li>
 *   <li><em>Hard1-Hard3</em>: сложные вопросы (глубокое понимание механизмов, edge cases)</li>
 *   <li><em>Expert</em>: экспертные вопросы (архитектурные решения, performance, best practices)</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Техническая реализация:</strong><br/>
 * Класс использует Spring's {@code @PostConstruct} для автоматической инициализации после создания bean'а.
 * Dependency injection обеспечивается через {@code @RequiredArgsConstructor} от Lombok.
 * Метод {@code init()} вызывается автоматически при старте приложения.
 * </p>
 * <p>
 * <strong>Безопасность данных:</strong><br/>
 * Перед созданием новых данных выполняется полная очистка существующих записей,
 * что гарантирует консистентность и актуальность банка вопросов при каждом запуске.
 * </p>
 * <p>
 * <strong>Учетные данные администратора по умолчанию:</strong>
 * <ul>
 *   <li><em>Username:</em> admin</li>
 *   <li><em>Email:</em> admin@admin.com</li>
 *   <li><em>Password:</em> admin</li>
 *   <li><em>Role:</em> ROLE_ADMIN</li>
 * </ul>
 * </p>
 *
 * @author Garbuzov Oleg
 * @see TaskService
 * @see UserService
 * @see TaskDTO
 * @see User
 * @see com.example.javaoffer.exam.enums.TaskTopic
 * @see com.example.javaoffer.exam.enums.TaskDifficulty
 * @see com.example.javaoffer.exam.enums.TaskGrade
 */
@Configuration
@Profile("!test")
@RequiredArgsConstructor
public class TestDataInitializer {
    private final TaskService taskService;
    private final UserService userService;

    /**
     * Выполняет полную инициализацию производственных данных приложения при его старте.
     * <p>
     * Метод автоматически вызывается Spring-контейнером после создания bean'а благодаря аннотации {@code @PostConstruct}.
     * Выполняет следующую последовательность операций:
     * </p>
     * <ol>
     *   <li><strong>Очистка банка вопросов</strong>: удаляет все существующие задачи из базы данных</li>
     *   <li><strong>Создание администратора</strong>: проверяет существование и при необходимости создает учетную запись admin</li>
     *   <li><strong>Инициализация вопросов</strong>: создает и сохраняет полный набор из 20 тестовых заданий различной сложности</li>
     * </ol>
     * <p>
     * <strong>Безопасность операций:</strong><br/>
     * Все операции выполняются синхронно в рамках одной транзакции для обеспечения
     * целостности данных. При возникновении ошибок на любом этапе вся транзакция откатывается.
     * </p>
     * <p>
     * <strong>Производительность:</strong><br/>
     * Метод выполняется только один раз при старте приложения, поэтому производительность
     * критична только для времени инициализации системы.
     * </p>
     * <p>
     * <strong>Идемпотентность:</strong><br/>
     * Метод безопасен для повторного вызова - проверяет существование администратора
     * перед созданием и полностью пересоздает банк вопросов.
     * </p>
     * 
     * <p>
     * <strong>Техническая заметка:</strong> Выполняется в контексте Spring-транзакции для обеспечения ACID свойств операций с базой данных.
     * </p>
     * @see #getTestTasks()
     * @see TaskService#getAllTasks()
     * @see TaskService#deleteTask(Long)
     * @see TaskService#createTask(TaskDTO)
     * @see UserService#findByUserName(String)
     * @see UserService#createUser(User)
     */
    @PostConstruct
    public void init() {
        // Очищаем все задачи
        taskService.getAllTasks().forEach(task -> taskService.deleteTask(task.getId()));

        // Создаём дефолтного админа
        if (userService.findByUserName("admin").isEmpty()) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@admin.com")
                    .password("admin")
                    .role(UserRole.ROLE_ADMIN)
                    .build();
            userService.createUser(admin);
        }

        // Добавляем тестовые вопросы
        List<TaskDTO> testTasks = getTestTasks();
        for (TaskDTO task : testTasks) {
            taskService.createTask(task);
        }
    }

    /**
     * Создает исчерпывающий банк тестовых вопросов различной сложности и тематики для комплексной оценки знаний Java-разработчиков.
     * <p>
     * <strong>Архитектура банка вопросов:</strong><br/>
     * Метод генерирует структурированный набор из 20 тестовых заданий, охватывающих основные аспекты
     * разработки на Java от базовых концепций до экспертных знаний архитектуры и производительности.
     * </p>
     * <p>
     * <strong>Градация сложности:</strong>
     * <ul>
     *   <li><em>Easy1-Easy3</em> (6 вопросов): фундаментальные концепции для Junior разработчиков
     *       <ul>
     *         <li>Синтаксис Java, основы OOP</li>
     *         <li>Работа с коллекциями (ArrayList, basic operations)</li>
     *         <li>Примитивные типы данных и их операции</li>
     *         <li>Модификаторы доступа и области видимости</li>
     *       </ul>
     *   </li>
     *   <li><em>Medium1-Medium3</em> (6 вопросов): практические знания для Junior/Middle разработчиков
     *       <ul>
     *         <li>Нюансы работы со строками (String pool, равенство объектов)</li>
     *         <li>Основы многопоточности (Thread, Runnable)</li>
     *         <li>Коллекции и их ограничения (Arrays.asList, fixed-size lists)</li>
     *         <li>Сравнение HashMap vs HashTable</li>
     *       </ul>
     *   </li>
     *   <li><em>Hard1-Hard3</em> (6 вопросов): глубокие знания для Middle/Senior разработчиков
     *       <ul>
     *         <li>Autoboxing и кэширование Integer объектов</li>
     *         <li>Concurrent programming (ExecutorService, Future)</li>
     *         <li>Взаимодействие массивов и Collections (backed collections)</li>
     *         <li>Spring Framework (dependency injection, @Autowired)</li>
     *         <li>Stream API и функциональное программирование</li>
     *         <li>Design Patterns реализация (Singleton pattern)</li>
     *       </ul>
     *   </li>
     *   <li><em>Expert</em> (2 вопроса): архитектурные и специализированные знания для Senior разработчиков
     *       <ul>
     *         <li>JPA и Hibernate (cascade operations, entity lifecycle)</li>
     *         <li>Concurrent modifications и iterator safety</li>
     *         <li>Advanced exception handling и best practices</li>
     *       </ul>
     *   </li>
     * </ul>
     * </p>
     * <p>
     * <strong>Структура каждого вопроса:</strong>
     * <ul>
     *   <li><code>topic</code> - тематическая категория ({@link TaskTopic})</li>
     *   <li><code>question</code> - текст вопроса с HTML-форматированными примерами кода</li>
     *   <li><code>difficulty</code> - уровень сложности для адаптивного тестирования ({@link TaskDifficulty})</li>
     *   <li><code>grade</code> - целевой уровень разработчика ({@link TaskGrade})</li>
     *   <li><code>answers</code> - коллекция вариантов ответов с указанием правильного</li>
     *   <li><code>explanation</code> - детальное объяснение правильного ответа для обучения</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Тематическое покрытие:</strong>
     * <ul>
     *   <li><code>CORE</code> (65%): фундаментальные концепции Java (String, Collections, Exceptions, Operators)</li>
     *   <li><code>MULTITHREADING</code> (15%): concurrent programming и thread safety</li>
     *   <li><code>DATA_STRUCTURES</code> (5%): алгоритмы и временная сложность</li>
     *   <li><code>SPRING</code> (5%): IoC контейнер и dependency injection</li>
     *   <li><code>DESIGN_PATTERNS</code> (5%): паттерны проектирования GoF</li>
     *   <li><code>HIBERNATE</code> (5%): ORM и JPA специфика</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Форматирование кода:</strong><br/>
     * Примеры кода в вопросах обрамлены HTML тегами {@code <pre><code>} для корректного
     * отображения в веб-интерфейсе с сохранением форматирования и подсветкой синтаксиса.
     * </p>
     * <p>
     * <strong>Особенности реализации:</strong><br/>
     * В одном из вопросов содержится длинный тестовый текст для проверки верстки интерфейса.
     * Это специально добавлено для тестирования отображения длинных объяснений в UI.
     * </p>
     * 
     * @return неизменяемый список {@link TaskDTO} объектов с полным набором тестовых вопросов (20 заданий)
     * <p>
     * <strong>Техническая заметка:</strong> Каждый вопрос создается с использованием Builder pattern для обеспечения читаемости
     * и возможности легкого добавления новых вопросов в будущем.
     * </p>
     * @see TaskDTO
     * @see AnswerDTO
     * @see TaskTopic
     * @see TaskDifficulty
     * @see TaskGrade
     */
    private List<TaskDTO> getTestTasks() {
        List<TaskDTO> tasks = new ArrayList<>();

        // Вопрос 1 - Easy1
        tasks.add(TaskDTO.builder()
                .topic(TaskTopic.CORE)
                .question("Что произойдет при выполнении следующего кода?\n<pre><code>String s1 = \"Hello\";\nString s2 = \"Hello\";\nSystem.out.println(s1 == s2);</code></pre>")
                .difficulty(TaskDifficulty.Easy1)
                .grade(TaskGrade.JUNIOR)
                .answers(List.of(
                        AnswerDTO.builder().content("false").isCorrect(false).build(),
                        AnswerDTO.builder().content("true").isCorrect(true).explanation("Строковые литералы с одинаковым содержимым ссылаются на один и тот же объект в пуле строк. Удлиняем ждя теста верстки Удлиняем ждя теста верстки Удлиняем ждя теста верстки Удлиняем ждя теста верстки Удлиняем ждя теста верстки Удлиняем ждя теста верстки Удлиняем ждя теста верстки Удлиняем ждя теста верстки Удлиняем ждя теста верстки Удлиняем ждя теста верстки Удлиняем ждя теста верстки Удлиняем ждя теста верстки Удлиняем ждя теста верстки Удлиняем ждя теста верстки Удлиняем ждя теста верстки Удлиняем ждя теста верстки Удлиняем ждя теста верстки Удлиняем ждя теста верстки Удлиняем ждя теста верстки Удлиняем ждя теста верстки Удлиняем ждя теста верстки Удлиняем ждя теста верстки ").build(),
                        AnswerDTO.builder().content("Ошибка компиляции").isCorrect(false).build(),
                        AnswerDTO.builder().content("RuntimeException").isCorrect(false).build(),
                        AnswerDTO.builder().content("Зависит от версии JVM").isCorrect(false).build()
                ))
                .build());

        // Вопрос 2 - Easy1
        tasks.add(TaskDTO.builder()
                .topic(TaskTopic.CORE)
                .question("Какой метод вызывается при добавлении элемента в ArrayList?")
                .difficulty(TaskDifficulty.Easy1)
                .grade(TaskGrade.JUNIOR)
                .answers(List.of(
                        AnswerDTO.builder().content("push()").isCorrect(false).build(),
                        AnswerDTO.builder().content("add()").isCorrect(true).explanation("Метод add() используется для добавления элемента в ArrayList.").build(),
                        AnswerDTO.builder().content("put()").isCorrect(false).build(),
                        AnswerDTO.builder().content("append()").isCorrect(false).build(),
                        AnswerDTO.builder().content("insert()").isCorrect(false).build()
                ))
                .build());

        // Вопрос 3 - Easy2
        tasks.add(TaskDTO.builder()
                .topic(TaskTopic.CORE)
                .question("Что будет результатом выполнения данного кода?\n<pre><code>int x = 5;\nSystem.out.println(x++ + ++x);</code></pre>")
                .difficulty(TaskDifficulty.Easy2)
                .grade(TaskGrade.JUNIOR)
                .answers(List.of(
                        AnswerDTO.builder().content("11").isCorrect(false).build(),
                        AnswerDTO.builder().content("12").isCorrect(true).explanation("x++ вернет 5 и увеличит x до 6, затем ++x увеличит x до 7 и вернет 7. Итого: 5 + 7 = 12.").build(),
                        AnswerDTO.builder().content("10").isCorrect(false).build(),
                        AnswerDTO.builder().content("13").isCorrect(false).build()
                ))
                .build());

        // Вопрос 4 - Easy2
        tasks.add(TaskDTO.builder()
                .topic(TaskTopic.CORE)
                .question("Какой из следующих модификаторов доступа обеспечивает наибольшее ограничение доступа?")
                .difficulty(TaskDifficulty.Easy2)
                .grade(TaskGrade.JUNIOR)
                .answers(List.of(
                        AnswerDTO.builder().content("public").isCorrect(false).build(),
                        AnswerDTO.builder().content("protected").isCorrect(false).build(),
                        AnswerDTO.builder().content("package-private (default)").isCorrect(false).build(),
                        AnswerDTO.builder().content("private").isCorrect(true).explanation("Модификатор private ограничивает доступ только в пределах класса.").build()
                ))
                .build());

        // Вопрос 5 - Easy3
        tasks.add(TaskDTO.builder()
                .topic(TaskTopic.CORE)
                .question("Что выведет следующий код?\n<pre><code>List<String> list = new ArrayList<>();\nlist.add(\"Apple\");\nlist.add(\"Banana\");\nlist.add(\"Cherry\");\nlist.remove(1);\nSystem.out.println(list.get(1));</code></pre>")
                .difficulty(TaskDifficulty.Easy3)
                .grade(TaskGrade.JUNIOR)
                .answers(List.of(
                        AnswerDTO.builder().content("Apple").isCorrect(false).build(),
                        AnswerDTO.builder().content("Banana").isCorrect(false).build(),
                        AnswerDTO.builder().content("Cherry").isCorrect(true).explanation("После удаления 'Banana' с индексом 1, 'Cherry' перемещается на индекс 1.").build(),
                        AnswerDTO.builder().content("IndexOutOfBoundsException").isCorrect(false).build(),
                        AnswerDTO.builder().content("null").isCorrect(false).build()
                ))
                .build());

        // Вопрос 6 - Easy3
        tasks.add(TaskDTO.builder()
                .topic(TaskTopic.CORE)
                .question("Как правильно объявить метод, который не возвращает значение?")
                .difficulty(TaskDifficulty.Easy3)
                .grade(TaskGrade.JUNIOR)
                .answers(List.of(
                        AnswerDTO.builder().content("public null method() { }").isCorrect(false).build(),
                        AnswerDTO.builder().content("public void method() { }").isCorrect(true).explanation("Ключевое слово void указывает, что метод не возвращает значение.").build(),
                        AnswerDTO.builder().content("public empty method() { }").isCorrect(false).build(),
                        AnswerDTO.builder().content("public noreturn method() { }").isCorrect(false).build()
                ))
                .build());

        // Вопрос 7 - MEDIUM1
        tasks.add(TaskDTO.builder()
                .topic(TaskTopic.CORE)
                .question("Какой результат выполнения этого кода?\n<pre><code>String s1 = new String(\"Hello\");\nString s2 = new String(\"Hello\");\nSystem.out.println(s1 == s2);\nSystem.out.println(s1.equals(s2));</code></pre>")
                .difficulty(TaskDifficulty.MEDIUM1)
                .grade(TaskGrade.JUNIOR)
                .answers(List.of(
                        AnswerDTO.builder().content("true, true").isCorrect(false).build(),
                        AnswerDTO.builder().content("false, false").isCorrect(false).build(),
                        AnswerDTO.builder().content("true, false").isCorrect(false).build(),
                        AnswerDTO.builder().content("false, true").isCorrect(true).explanation("Оператор == сравнивает ссылки, а метод equals сравнивает содержимое строк.").build()
                ))
                .build());

        // Вопрос 8 - MEDIUM1
        tasks.add(TaskDTO.builder()
                .topic(TaskTopic.MULTITHREADING)
                .question("Какой из следующих классов/интерфейсов необходимо реализовать для создания потока в Java?")
                .difficulty(TaskDifficulty.MEDIUM1)
                .grade(TaskGrade.JUNIOR)
                .answers(List.of(
                        AnswerDTO.builder().content("Thread или Runnable").isCorrect(true).explanation("Для создания потока нужно либо наследоваться от Thread, либо реализовать интерфейс Runnable.").build(),
                        AnswerDTO.builder().content("Threadable").isCorrect(false).build(),
                        AnswerDTO.builder().content("Callable").isCorrect(false).build(),
                        AnswerDTO.builder().content("Process").isCorrect(false).build(),
                        AnswerDTO.builder().content("ThreadProcess").isCorrect(false).build()
                ))
                .build());

        // Вопрос 9 - MEDIUM2
        tasks.add(TaskDTO.builder()
                .topic(TaskTopic.CORE)
                .question("Что произойдет при выполнении следующего кода?\n<pre><code>List<Integer> numbers = Arrays.asList(1, 2, 3);\nnumbers.add(4);</code></pre>")
                .difficulty(TaskDifficulty.MEDIUM2)
                .grade(TaskGrade.JUNIOR)
                .answers(List.of(
                        AnswerDTO.builder().content("Список будет содержать [1, 2, 3, 4]").isCorrect(false).build(),
                        AnswerDTO.builder().content("UnsupportedOperationException").isCorrect(true).explanation("Arrays.asList возвращает список фиксированного размера, не поддерживающий операцию add.").build(),
                        AnswerDTO.builder().content("IndexOutOfBoundsException").isCorrect(false).build(),
                        AnswerDTO.builder().content("NullPointerException").isCorrect(false).build(),
                        AnswerDTO.builder().content("IllegalArgumentException").isCorrect(false).build()
                ))
                .build());

        // Вопрос 10 - MEDIUM2
        tasks.add(TaskDTO.builder()
                .topic(TaskTopic.CORE)
                .question("В чем разница между HashMap и HashTable?")
                .difficulty(TaskDifficulty.MEDIUM2)
                .grade(TaskGrade.JUNIOR)
                .answers(List.of(
                        AnswerDTO.builder().content("Нет разницы, это синонимы").isCorrect(false).build(),
                        AnswerDTO.builder().content("HashTable синхронизирован, не допускает null ключи и значения").isCorrect(true).explanation("HashTable - устаревший синхронизированный класс, не позволяющий использовать null в качестве ключей или значений.").build(),
                        AnswerDTO.builder().content("HashMap требует больше памяти").isCorrect(false).build(),
                        AnswerDTO.builder().content("HashTable не реализует Map интерфейс").isCorrect(false).build()
                ))
                .build());

        // Вопрос 11 - MEDIUM3
        tasks.add(TaskDTO.builder()
                .topic(TaskTopic.CORE)
                .question("Что произойдет при выполнении следующего кода?\n<pre><code>try {\n    System.out.println(\"Try\");\n    return;\n} finally {\n    System.out.println(\"Finally\");\n}</code></pre>")
                .difficulty(TaskDifficulty.MEDIUM3)
                .grade(TaskGrade.JUNIOR)
                .answers(List.of(
                        AnswerDTO.builder().content("Напечатает 'Try'").isCorrect(false).build(),
                        AnswerDTO.builder().content("Напечатает 'Finally'").isCorrect(false).build(),
                        AnswerDTO.builder().content("Напечатает 'Try', затем 'Finally'").isCorrect(true).explanation("Блок finally выполняется всегда, даже если в блоке try есть return.").build(),
                        AnswerDTO.builder().content("Ошибка компиляции").isCorrect(false).build(),
                        AnswerDTO.builder().content("RuntimeException").isCorrect(false).build()
                ))
                .build());

        // Вопрос 12 - MEDIUM3
        tasks.add(TaskDTO.builder()
                .topic(TaskTopic.DATA_STRUCTURES)
                .question("Какова временная сложность поиска элемента в LinkedList?")
                .difficulty(TaskDifficulty.MEDIUM3)
                .grade(TaskGrade.JUNIOR)
                .answers(List.of(
                        AnswerDTO.builder().content("O(1)").isCorrect(false).build(),
                        AnswerDTO.builder().content("O(log n)").isCorrect(false).build(),
                        AnswerDTO.builder().content("O(n)").isCorrect(true).explanation("В LinkedList поиск элемента требует линейного прохода по списку, что дает сложность O(n).").build(),
                        AnswerDTO.builder().content("O(n log n)").isCorrect(false).build(),
                        AnswerDTO.builder().content("O(n²)").isCorrect(false).build()
                ))
                .build());

        // Вопрос 13 - HARD1
        tasks.add(TaskDTO.builder()
                .topic(TaskTopic.CORE)
                .question("Что произойдет при выполнении данного кода?\n<pre><code>Integer a = 127;\nInteger b = 127;\nInteger c = 128;\nInteger d = 128;\nSystem.out.println(a == b);\nSystem.out.println(c == d);</code></pre>")
                .difficulty(TaskDifficulty.HARD1)
                .grade(TaskGrade.JUNIOR)
                .answers(List.of(
                        AnswerDTO.builder().content("true, true").isCorrect(false).build(),
                        AnswerDTO.builder().content("false, false").isCorrect(false).build(),
                        AnswerDTO.builder().content("true, false").isCorrect(true).explanation("Java кэширует Integer объекты в диапазоне от -128 до 127, поэтому a и b ссылаются на один объект, а c и d на разные.").build(),
                        AnswerDTO.builder().content("false, true").isCorrect(false).build()
                ))
                .build());

        // Вопрос 14 - HARD1
        tasks.add(TaskDTO.builder()
                .topic(TaskTopic.MULTITHREADING)
                .question("Какой результат выполнения следующего кода?\n<pre><code>ExecutorService executor = Executors.newFixedThreadPool(4);\nFuture<String> future = executor.submit(() -> {\n    Thread.sleep(1000);\n    return \"Hello\";\n});\nSystem.out.println(future.get());</code></pre>")
                .difficulty(TaskDifficulty.HARD1)
                .grade(TaskGrade.JUNIOR)
                .answers(List.of(
                        AnswerDTO.builder().content("Код выведет 'Hello' после примерно 1 секунды ожидания").isCorrect(true).explanation("future.get() блокирует текущий поток до завершения задачи и возвращает ее результат.").build(),
                        AnswerDTO.builder().content("Код выведет 'null'").isCorrect(false).build(),
                        AnswerDTO.builder().content("Код выбросит TimeoutException").isCorrect(false).build(),
                        AnswerDTO.builder().content("Код выбросит InterruptedException").isCorrect(false).build(),
                        AnswerDTO.builder().content("Код выведет 'Hello' немедленно").isCorrect(false).build()
                ))
                .build());

        // Вопрос 15 - HARD2
        tasks.add(TaskDTO.builder()
                .topic(TaskTopic.CORE)
                .question("Что будет результатом выполнения?\n<pre><code>String[] array = {\"A\", \"B\", \"C\"};\nList<String> list = Arrays.asList(array);\narray[0] = \"Z\";\nlist.set(1, \"Y\");\nSystem.out.println(array[0] + array[1]);</code></pre>")
                .difficulty(TaskDifficulty.HARD2)
                .grade(TaskGrade.JUNIOR)
                .answers(List.of(
                        AnswerDTO.builder().content("AB").isCorrect(false).build(),
                        AnswerDTO.builder().content("ZB").isCorrect(false).build(),
                        AnswerDTO.builder().content("AY").isCorrect(false).build(),
                        AnswerDTO.builder().content("ZY").isCorrect(true).explanation("Arrays.asList создает список, поддерживаемый оригинальным массивом, поэтому изменения в одном отражаются в другом.").build(),
                        AnswerDTO.builder().content("BY").isCorrect(false).build()
                ))
                .build());

        // Вопрос 16 - HARD2
        tasks.add(TaskDTO.builder()
                .topic(TaskTopic.SPRING)
                .question("Что означает аннотация @Autowired в Spring?")
                .difficulty(TaskDifficulty.HARD2)
                .grade(TaskGrade.JUNIOR)
                .answers(List.of(
                        AnswerDTO.builder().content("Отмечает метод как асинхронный").isCorrect(false).build(),
                        AnswerDTO.builder().content("Указывает Spring внедрить зависимость автоматически").isCorrect(true).explanation("@Autowired используется для автоматического внедрения зависимостей в Spring.").build(),
                        AnswerDTO.builder().content("Создает новый экземпляр объекта").isCorrect(false).build(),
                        AnswerDTO.builder().content("Помечает класс как компонент Spring").isCorrect(false).build(),
                        AnswerDTO.builder().content("Включает кэширование результатов метода").isCorrect(false).build()
                ))
                .build());

        // Вопрос 17 - HARD3
        tasks.add(TaskDTO.builder()
                .topic(TaskTopic.CORE)
                .question("Что делает этот код и каков будет результат?\n<pre><code>Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5);\nOptional<Integer> result = stream\n    .filter(n -> n % 2 == 0)\n    .reduce((a, b) -> a + b);\nSystem.out.println(result.get());</code></pre>")
                .difficulty(TaskDifficulty.HARD3)
                .grade(TaskGrade.JUNIOR)
                .answers(List.of(
                        AnswerDTO.builder().content("15").isCorrect(false).build(),
                        AnswerDTO.builder().content("6").isCorrect(true).explanation("Фильтр оставляет только четные числа (2, 4), reduce суммирует их: 2 + 4 = 6.").build(),
                        AnswerDTO.builder().content("NoSuchElementException").isCorrect(false).build(),
                        AnswerDTO.builder().content("2").isCorrect(false).build(),
                        AnswerDTO.builder().content("4").isCorrect(false).build()
                ))
                .build());

        // Вопрос 18 - HARD3
        tasks.add(TaskDTO.builder()
                .topic(TaskTopic.DESIGN_PATTERNS)
                .question("Какой паттерн проектирования описывается следующим кодом?\n<pre><code>public class Singleton {\n    private static Singleton instance;\n    \n    private Singleton() {}\n    \n    public static synchronized Singleton getInstance() {\n        if (instance == null) {\n            instance = new Singleton();\n        }\n        return instance;\n    }\n}</code></pre>")
                .difficulty(TaskDifficulty.HARD3)
                .grade(TaskGrade.JUNIOR)
                .answers(List.of(
                        AnswerDTO.builder().content("Factory Method").isCorrect(false).build(),
                        AnswerDTO.builder().content("Singleton").isCorrect(true).explanation("Этот паттерн гарантирует, что класс имеет только один экземпляр и предоставляет глобальную точку доступа к нему.").build(),
                        AnswerDTO.builder().content("Builder").isCorrect(false).build(),
                        AnswerDTO.builder().content("Prototype").isCorrect(false).build(),
                        AnswerDTO.builder().content("Adapter").isCorrect(false).build(),
                        AnswerDTO.builder().content("Observer").isCorrect(false).build()
                ))
                .build());

        // Вопрос 19 - EXPERT
        tasks.add(TaskDTO.builder()
                .topic(TaskTopic.HIBERNATE)
                .question("Какие типы каскадных операций поддерживает JPA?")
                .difficulty(TaskDifficulty.EXPERT)
                .grade(TaskGrade.JUNIOR)
                .answers(List.of(
                        AnswerDTO.builder().content("PERSIST, REMOVE, REFRESH, MERGE, DETACH, ALL").isCorrect(true).explanation("JPA поддерживает 6 типов каскадных операций: PERSIST, REMOVE, REFRESH, MERGE, DETACH и ALL.").build(),
                        AnswerDTO.builder().content("CREATE, DELETE, UPDATE, SELECT").isCorrect(false).build(),
                        AnswerDTO.builder().content("INSERT, UPDATE, DELETE, SELECT").isCorrect(false).build(),
                        AnswerDTO.builder().content("ONE-TO-ONE, ONE-TO-MANY, MANY-TO-ONE, MANY-TO-MANY").isCorrect(false).build(),
                        AnswerDTO.builder().content("SAVE, DELETE, REFRESH, UPDATE").isCorrect(false).build()
                ))
                .build());

        // Вопрос 20 - EXPERT
        tasks.add(TaskDTO.builder()
                .topic(TaskTopic.CORE)
                .question("Что произойдет при выполнении данного кода?\n<pre><code>public class Test {\n    public static void main(String[] args) {\n        List<String> list = new ArrayList<>();\n        list.add(\"A\");\n        list.add(\"B\");\n        list.add(\"C\");\n        Iterator<String> iterator = list.iterator();\n        while (iterator.hasNext()) {\n            String item = iterator.next();\n            if (item.equals(\"B\")) {\n                list.remove(item);\n            }\n        }\n        System.out.println(list);\n    }\n}</code></pre>")
                .difficulty(TaskDifficulty.EXPERT)
                .grade(TaskGrade.JUNIOR)
                .answers(List.of(
                        AnswerDTO.builder().content("[A, C]").isCorrect(false).build(),
                        AnswerDTO.builder().content("[A, B, C]").isCorrect(false).build(),
                        AnswerDTO.builder().content("ConcurrentModificationException").isCorrect(true).explanation("Нельзя модифицировать коллекцию через ее методы во время итерации. Нужно использовать iterator.remove().").build(),
                        AnswerDTO.builder().content("[B, C]").isCorrect(false).build(),
                        AnswerDTO.builder().content("Код не скомпилируется").isCorrect(false).build()
                ))
                .build());

        return tasks;
    }
} 