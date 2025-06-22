package com.example.javaoffer.controllerTest;

import com.example.javaoffer.admin.dto.ImportExportHistoryDTO;
import com.example.javaoffer.admin.entity.ImportExportHistory;
import com.example.javaoffer.admin.repository.ImportExportHistoryRepository;
import com.example.javaoffer.user.entity.User;
import com.example.javaoffer.user.enums.UserRole;
import com.example.javaoffer.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static TestConstants.TestConstant.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тестовый класс для контроллера истории операций импорта и экспорта.
 * <p>
 * Проверяет функциональность управления историей операций импорта/экспорта
 * в административной панели:
 * <ul>
 *   <li>Отображение страницы истории операций</li>
 *   <li>Корректность данных в списке истории</li>
 *   <li>Очистка истории операций</li>
 *   <li>Контроль доступа для ролей пользователей</li>
 * </ul>
 * </p>
 * <p>
 * Тестирует операции для импорта и экспорта задач системы,
 * включая статусы успешного и неуспешного выполнения операций.
 * </p>
 *
 * @author Garbuzov Oleg
 */
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ImportExportHistoryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ImportExportHistoryRepository historyRepository;
	
	@Autowired
	private UserRepository userRepository;

	/**
	 * Настройка тестовых данных перед каждым тестом.
	 * <p>
	 * Очищает базу данных от предыдущих записей и создает свежие
	 * тестовые данные для изолированного выполнения тестов.
	 * </p>
	 */
	@BeforeEach
	void setUp() {
		log.debug("Настройка тестовых данных для контроллера истории импорта/экспорта");
		historyRepository.deleteAll();
		log.trace("Очищены все записи истории импорта/экспорта");
		createTestData();
		log.debug("Тестовые данные успешно созданы");
	}

	/**
	 * Создает тестовые данные для проверки функциональности контроллера.
	 * <p>
	 * Создает тестового администратора и две записи истории операций:
	 * <ul>
	 *   <li>Успешный экспорт данных</li>
	 *   <li>Неуспешный импорт данных с ошибкой</li>
	 * </ul>
	 * </p>
	 */
	private void createTestData() {
		log.debug("Создание тестовых данных пользователя и истории операций");
		
		User admin = User.builder()
				.username("admin")
				.email("admin@test.com")
				.password("password")
				.role(UserRole.ROLE_ADMIN)
				.build();
		
		admin = userRepository.save(admin);
		log.trace("Создан тестовый администратор с ID: {}", admin.getId());

		ImportExportHistory exportHistory = ImportExportHistory.builder()
				.user(admin)
				.operationType(OPERATION_TYPE_EXPORT)
				.fileName(FILE_NAME_EXPORT)
				.status(STATUS_SUCCESS)
				.startedAt(LocalDateTime.now().minusHours(1))
				.finishedAt(LocalDateTime.now().minusMinutes(50))
				.result(RESULT_SUCCESS)
				.build();

		ImportExportHistory importHistory = ImportExportHistory.builder()
				.user(admin)
				.operationType(OPERATION_TYPE_IMPORT)
				.fileName(FILE_NAME_IMPORT)
				.status(STATUS_ERROR)
				.startedAt(LocalDateTime.now().minusMinutes(30))
				.finishedAt(LocalDateTime.now().minusMinutes(25))
				.result(RESULT_ERROR)
				.build();

		historyRepository.save(exportHistory);
		historyRepository.save(importHistory);
		log.debug("Созданы записи истории: 1 успешный экспорт, 1 неуспешный импорт");
	}

	/**
	 * Тестирует корректное отображение страницы истории импорта/экспорта.
	 * <p>
	 * Проверяет, что страница истории операций:
	 * <ul>
	 *   <li>Успешно загружается с правильным шаблоном</li>
	 *   <li>Содержит все необходимые атрибуты модели</li>
	 *   <li>Отображает корректное количество записей</li>
	 *   <li>Содержит записи с ожидаемыми данными</li>
	 * </ul>
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@Test
	@DisplayName("Страница истории импорта/экспорта должна отображаться корректно")
	@WithMockUser(roles = "ADMIN")
	void getHistoryPageShouldDisplayCorrectly() throws Exception {
		log.info("Тестирование отображения страницы истории импорта/экспорта");

		log.debug("Отправка GET запроса на страницу истории");
		MvcResult result = mockMvc.perform(get(URL_ADMIN_IMPORT_EXPORT_HISTORY)
						.with(SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/import-export-history"))
				.andExpect(model().attributeExists("historyList"))
				.andExpect(model().attributeExists("request"))
				.andReturn();

		@SuppressWarnings("unchecked")
		List<ImportExportHistoryDTO> historyList = (List<ImportExportHistoryDTO>) 
			result.getModelAndView().getModel().get("historyList");
		
		log.debug("Получен список истории с {} записями", historyList.size());
		assertEquals(2, historyList.size(), "Должно быть 2 записи в истории");

		boolean hasExport = false;
		boolean hasImport = false;

		log.trace("Проверка содержимого записей истории");
		for (ImportExportHistoryDTO history : historyList) {
			if (OPERATION_TYPE_EXPORT.equals(history.getOperationType()) &&
					FILE_NAME_EXPORT.equals(history.getFileName()) &&
					STATUS_SUCCESS.equals(history.getStatus())) {
				hasExport = true;
				log.trace("Найдена запись успешного экспорта");
			}

			if (OPERATION_TYPE_IMPORT.equals(history.getOperationType()) &&
					FILE_NAME_IMPORT.equals(history.getFileName()) &&
					STATUS_ERROR.equals(history.getStatus())) {
				hasImport = true;
				log.trace("Найдена запись неуспешного импорта");
			}
		}

		assertTrue(hasExport, "Должна быть запись об экспорте");
		assertTrue(hasImport, "Должна быть запись об импорте");
		
		log.info("Тестирование отображения страницы истории завершено успешно");
	}

	/**
	 * Тестирует корректную работу очистки истории импорта/экспорта.
	 * <p>
	 * Проверяет, что операция очистки истории:
	 * <ul>
	 *   <li>Корректно удаляет все записи из базы данных</li>
	 *   <li>Возвращает правильный статус перенаправления</li>
	 *   <li>Перенаправляет на страницу истории</li>
	 * </ul>
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@Test
	@DisplayName("Очистка истории импорта/экспорта должна работать корректно")
	@WithMockUser(roles = "ADMIN")
	void clearHistoryShouldWorkCorrectly() throws Exception {
		log.info("Тестирование функции очистки истории импорта/экспорта");

		log.debug("Проверка наличия записей в базе данных перед очисткой");
		List<ImportExportHistory> beforeClear = historyRepository.findAll();
		assertFalse(beforeClear.isEmpty(), "База должна содержать записи до очистки");
		assertEquals(2, beforeClear.size(), "База должна содержать 2 записи до очистки");
		log.trace("Подтверждено наличие {} записей перед очисткой", beforeClear.size());

		log.debug("Отправка POST запроса на очистку истории");
		mockMvc.perform(post(URL_ADMIN_IMPORT_EXPORT_HISTORY_CLEAR)
						.with(SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl(URL_ADMIN_IMPORT_EXPORT_HISTORY));

		log.debug("Проверка состояния базы данных после очистки");
		List<ImportExportHistory> afterClear = historyRepository.findAll();
		assertTrue(afterClear.isEmpty(), "База должна быть пустой после очистки");
		log.trace("Подтверждено удаление всех записей: {} записей осталось", afterClear.size());
		
		log.info("Тестирование очистки истории завершено успешно");
	}

	/**
	 * Тестирует контроль доступа к истории импорта/экспорта.
	 * <p>
	 * Проверяет, что пользователи без административных прав
	 * не могут получить доступ к странице истории операций.
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@Test
	@DisplayName("Доступ к истории импорта/экспорта без прав администратора должен быть запрещен")
	@WithMockUser(roles = "USER")
	void accessToHistoryPageWithoutAdminRoleShouldBeForbidden() throws Exception {
		log.info("Тестирование контроля доступа к истории импорта/экспорта");

		log.debug("Попытка доступа к истории под ролью USER");
		mockMvc.perform(get(URL_ADMIN_IMPORT_EXPORT_HISTORY)
						.with(SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().isForbidden());
		
		log.debug("Подтвержден запрет доступа для роли USER");
		log.info("Тестирование контроля доступа завершено успешно");
	}
} 