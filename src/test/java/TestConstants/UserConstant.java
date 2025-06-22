package TestConstants;

/**
 * Класс содержит константы для тестирования пользователей и системы аутентификации.
 * <p>
 * Включает в себя:
 * <ul>
 *   <li>Тестовые данные администраторов и пользователей</li>
 *   <li>Учетные данные для различных тестовых сценариев</li>
 *   <li>Невалидные данные для негативного тестирования</li>
 *   <li>Константы ролей пользователей</li>
 * </ul>
 * </p>
 *
 * @author Garbuzov Oleg
 * @since 1.0
 */
public final class UserConstant {

	/**
	 * Приватный конструктор для предотвращения создания экземпляров утилитного класса.
	 */
	private UserConstant() {
		throw new AssertionError("Utility class cannot be instantiated");
	}

	// === Данные тестовых пользователей ===

	/**
	 * Имя тестового пользователя
	 */
	public static final String USER_NAME = "testUser";

	/**
	 * Имя второго тестового пользователя
	 */
	public static final String USER_NAME_2 = "testUser2";

	/**
	 * Пароль тестового пользователя
	 */
	public static final String USER_PASSWORD = "testPassword";

	/**
	 * Альтернативный пароль для тестов
	 */
	public static final String USER_PASSWORD_2 = "testPassword2";

	/**
	 * Email тестового пользователя
	 */
	public static final String USER_EMAIL = "testUser@test.dev";

	/**
	 * Email второго тестового пользователя
	 */
	public static final String USER_EMAIL_2 = "testUser2@test.dev";

	/**
	 * Строковое представление роли пользователя
	 */
	public static final String USER_ROLE_STR = "USER";

	// === Невалидные данные для негативного тестирования ===

	/**
	 * Недопустимое имя пользователя (слишком короткое)
	 */
	public static final String USER_NAME_NON_VALID = "us";

	/**
	 * Недопустимый пароль (слишком короткий)
	 */
	public static final String USER_PASSWORD_NON_VALID = "pas";

	/**
	 * Недопустимый email (отсутствует символ @)
	 */
	public static final String USER_EMAIL_NON_VALID_1 = "testUser-test.dev";

	/**
	 * Недопустимый email (неправильный домен)
	 */
	public static final String USER_EMAIL_NON_VALID_2 = "testUser@testdev";
}
