package com.example.javaoffer.exam.controller;

import com.example.javaoffer.common.utils.ClientUtils;
import com.example.javaoffer.exam.cache.exception.NoEntryInCacheException;
import com.example.javaoffer.exam.cache.service.ExamSessionCacheService;
import com.example.javaoffer.exam.dto.*;
import com.example.javaoffer.exam.enums.ExamDifficulty;
import com.example.javaoffer.exam.enums.ExamMode;
import com.example.javaoffer.exam.logic.StartExamBuilder;
import com.example.javaoffer.exam.service.ExamService;
import com.example.javaoffer.exam.validation.ExamStartProcessValidation;
import com.example.javaoffer.rateLimiter.annotation.RateLimit;
import com.google.common.util.concurrent.Striped;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

import static com.example.javaoffer.common.constants.UrlConstant.*;
import static com.example.javaoffer.common.constants.ViewConstant.*;

/**
 * Контроллер для обработки запросов к режиму тестирования.
 * Предоставляет эндпоинты для начала экзамена, получения следующего вопроса,
 * проверки ответов и прерывания тестирования.
 * <p>
 * Реализует потокобезопасную обработку запросов с помощью Striped Locks,
 * что обеспечивает блокировку только для одинаковых пользовательских сессий.
 *
 * @author Garbuzov Oleg
 */
@Slf4j
@Controller
@RequestMapping(URL_EXAM_ROOT)
@RequiredArgsConstructor
public class ExamController {

	private final ExamService examService;
	private final Striped<Lock> locks = Striped.lock(10240);
	private final ExamSessionCacheService examSessionCacheService;
	private final StartExamBuilder startExamBuilder;
	private final Set<ExamStartProcessValidation> examStartProcessValidations;

	/**
	 * Начинает новый экзамен в выбранном режиме.
	 * Выполняет валидацию на возможность начала экзамена в выбранном режиме.
	 *
	 * @param model    модель для передачи данных в представление
	 * @param request  HTTP запрос
	 * @param examMode строковое представление режима экзамена
	 * @return имя представления для отображения страницы экзамена
	 */
	@PostMapping(URL_START_EXAM)
	@RateLimit
	public String startExam(Model model,
							HttpServletRequest request,
							@RequestParam String examMode) {
		String clientIp = ClientUtils.getClientIp(request);
		log.info("{}:POST {}, начало экзамена. Запускаем валидацию старта экзамена", clientIp, URL_EXAM_ROOT + URL_START_EXAM);

		examStartProcessValidations.forEach(
				examProcessValidation -> examProcessValidation.validation(ExamMode.valueOf(examMode)));

		return processStartExamRequest(model, request, ExamMode.valueOf(examMode), ExamDifficulty.EASY);
	}

	/**
	 * Обрабатывает запрос на получение следующего вопроса в экзамене.
	 * Использует блокировку для предотвращения одновременного доступа
	 * к одной и той же сессии экзамена.
	 *
	 * @param examRequest запрос на следующий вопрос
	 * @param request     HTTP запрос
	 * @param model       модель для передачи данных в представление
	 * @return имя представления для отображения блока вопроса
	 * @throws NoEntryInCacheException если сессия экзамена не найдена в кэше
	 */
	@PostMapping(URL_NEXT_QUESTION)
	@RateLimit
	public String nextQuestion(@RequestBody @Valid ExamNextQuestionRequestDTO examRequest,
							   HttpServletRequest request,
							   Model model) {
		String clientIp = ClientUtils.getClientIp(request);

		// Получаем объект, по которому будем лочить
		Object lockKey = ClientUtils.getLockKeyByXsrfOrIp(request);
		Lock lock = locks.get(lockKey);
		lock.lock();
		try {
			UUID examId = examRequest.getExamId();
			ExamMode examMode = examSessionCacheService.getExamMode(examId).orElseThrow(() ->
					new NoEntryInCacheException(
							"Ошибка. Сессия не найдена. Вероятно вас долго не было. Начните сначала"
					));
			log.debug("{}:POST {}: Запрос следующего вопроса для examId={}", clientIp, URL_EXAM_ROOT + URL_NEXT_QUESTION, examId);

			ExamResumeResponseDTO result = examService.startOrResumeExam(examRequest, examMode);
			model.addAttribute("examId", examId);
			model.addAttribute("result", result);

			log.info("{}:POST {}: examId={}, taskId={}, difficulty={}, requestId:{}",
					clientIp,
					URL_EXAM_ROOT + URL_NEXT_QUESTION,
					examId,
					result.getTaskDto().getId(),
					result.getStats().getCurrentDifficulty(),
					examRequest.getRequestId());

			return VIEW_TEMPLATE_QUESTION_BLOCK;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Проверяет правильность ответа пользователя на вопрос экзамена.
	 * Использует блокировку для предотвращения одновременного доступа
	 * к одной и той же сессии экзамена.
	 *
	 * @param examRequest запрос на проверку ответа
	 * @param request     HTTP запрос
	 * @return ResponseEntity с результатом проверки ответа
	 * @throws NoEntryInCacheException если сессия экзамена не найдена в кэше
	 */
	@PostMapping(URL_ANSWER_CHECK)
	@ResponseBody
	@RateLimit
	public ResponseEntity<ValidateAnswerResponseDTO> checkAnswer(@RequestBody @Valid ExamCheckAnswerRequestDTO examRequest,
																 HttpServletRequest request) {
		String clientIp = ClientUtils.getClientIp(request);

		// Получаем объект, по которому будем лочить
		Object lockKey = ClientUtils.getLockKeyByXsrfOrIp(request);
		Lock lock = locks.get(lockKey);
		lock.lock();
		try {
			UUID examId = examRequest.getExamId();
			ExamMode examMode = examSessionCacheService.getExamMode(examId).orElseThrow(() ->
					new NoEntryInCacheException(
							"Ошибка. Сессия не найдена. Вероятно вас долго не было. Начните сначала"
					));
			log.debug("{}:POST {}: Запрос правильного ответа для examId={}", clientIp, URL_EXAM_ROOT + URL_ANSWER_CHECK, examId);

			ValidateAnswerResponseDTO result = examService.answerValidationAndUpdateStats(examRequest, examMode);

			log.info("{}:POST {}: examId={}, верный ответ={}, ответ пользователя={}, requestId:{}",
					clientIp,
					URL_EXAM_ROOT + URL_ANSWER_CHECK,
					examId,
					result.getId(),
					examRequest.getSelectedAnswer(),
					examRequest.getRequestId());

			return ResponseEntity.ok(result);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Прерывает текущий экзамен и возвращает результаты.
	 * Использует блокировку для предотвращения одновременного доступа
	 * к одной и той же сессии экзамена.
	 *
	 * @param model          модель для передачи данных в представление
	 * @param request        HTTP запрос
	 * @param examRequestDTO запрос на прерывание экзамена
	 * @return имя представления для отображения результатов экзамена
	 * @throws NoEntryInCacheException если сессия экзамена не найдена в кэше
	 */
	@PostMapping(URL_ABORT_EXAM)
	@RateLimit
	public String abortExam(Model model,
							HttpServletRequest request,
							@RequestBody @Valid ExamAbortRequestDTO examRequestDTO) {
		String clientIp = ClientUtils.getClientIp(request);
		log.info("{}:POST {}, запрос на прерывание экзамена:{}", clientIp, URL_EXAM_ROOT + URL_ABORT_EXAM, examRequestDTO.getExamId());

		// Получаем объект, по которому будем лочить
		Object lockKey = ClientUtils.getLockKeyByXsrfOrIp(request);
		Lock lock = locks.get(lockKey);
		lock.lock();
		try {
			UUID examId = examRequestDTO.getExamId();
			ExamMode examMode = examSessionCacheService.getExamMode(examId).orElseThrow(() ->
					new NoEntryInCacheException(
							"Ошибка. Сессия не найдена. Вероятно вас долго не было. Начните сначала"
					));

			ExamAbortResponseDTO result = examService.abortExam(examRequestDTO, examMode);

			model.addAttribute("examId", examId);
			model.addAttribute("result", result);

			return VIEW_TEMPLATE_EXAM_RESULT_BLOCK;

		} finally {
			lock.unlock();
		}
	}

	/**
	 * Обрабатывает запрос на начало нового экзамена.
	 * Создает запрос для получения первого вопроса и инициализирует модель.
	 *
	 * @param model          модель для передачи данных в представление
	 * @param request        HTTP запрос
	 * @param examMode       выбранный режим экзамена
	 * @param examDifficulty начальная сложность экзамена
	 * @return имя представления для отображения страницы экзамена
	 */
	private String processStartExamRequest(
			Model model,
			HttpServletRequest request,
			ExamMode examMode,
			ExamDifficulty examDifficulty) {
		String clientIp = ClientUtils.getClientIp(request);
		log.debug("{}:processExamRequest: Обработка запроса для старта экзамена.", clientIp);

		//Билдим ExamRequestDTO
		ExamNextQuestionRequestDTO examRequest = startExamBuilder.buildAndGet(examMode, examDifficulty);

		ExamResumeResponseDTO result = examService.startOrResumeExam(examRequest, examMode);
		log.debug("{}:processExamRequest: Получен ответ для старта экзамена examId={}, taskId={}, сложность={}",
				clientIp, examRequest.getExamId(), result.getTaskDto().getId(), result.getStats().getCurrentDifficulty());

		return buildModelAndReturnViewForExam(model, result, examRequest, request, examMode);
	}

	/**
	 * Формирует модель и возвращает имя представления для отображения экзамена.
	 *
	 * @param model                 модель для передачи данных в представление
	 * @param examResumeResponseDTO ответ с данными для возобновления экзамена
	 * @param examRequest           исходный запрос на начало экзамена
	 * @param request               HTTP запрос
	 * @param examMode              выбранный режим экзамена
	 * @return имя представления для отображения страницы экзамена
	 */
	private String buildModelAndReturnViewForExam(Model model,
												  ExamResumeResponseDTO examResumeResponseDTO,
												  ExamNextQuestionRequestDTO examRequest,
												  HttpServletRequest request,
												  ExamMode examMode) {
		String clientIp = ClientUtils.getClientIp(request);
		model.addAttribute("result", examResumeResponseDTO);
		model.addAttribute("examId", examRequest.getExamId());
		model.addAttribute("request", request);
		log.debug("{}:buildModelAndReturnView: Построение модели для examId={}, mode={}. Начало экзамена",
				clientIp, examRequest.getExamId(), examMode);

		return VIEW_TEMPLATE_EXAM;
	}
}
