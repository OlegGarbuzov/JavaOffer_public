package com.example.javaoffer.admin.repository;

import com.example.javaoffer.admin.entity.ImportExportHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий для работы с сущностью {@link ImportExportHistory}.
 * <p>
 * Предоставляет методы для выполнения CRUD-операций и более сложных запросов
 * к таблице истории импорта/экспорта вопросов.
 * 
 *
 * @author Garbuzov Oleg
 */
@Repository
public interface ImportExportHistoryRepository extends JpaRepository<ImportExportHistory, Long> {
}