package com.lekseradar.service;

import com.lekseradar.dto.homework.AssignmentResponseDTO;
import com.lekseradar.exception.BadRequestException;
import com.lekseradar.exception.NotFoundException;
import com.lekseradar.model.AssignmentEntity;
import com.lekseradar.model.enums.AssignmentStatusEnum;
import com.lekseradar.repository.AssignmentRepository;
import com.lekseradar.repository.UserEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Домашние задания ученика: список, одно задание, отметка «сдано». Собирает их collect() (homework-collector). */
@Service
public class HomeworkService {
    private static final int LIST_DESCRIPTION = 300;

    private final AssignmentRepository assignments;
    private final UserEventRepository events;

    public HomeworkService(AssignmentRepository assignments, UserEventRepository events) {
        this.assignments = assignments;
        this.events = events;
    }

    /** @param status null — все; overdue — todo с прошедшим дедлайном */
    public List<AssignmentResponseDTO> list(Long userId, AssignmentStatusEnum status) {
        return assignments.findForUser(userId).stream()
                .map(a -> AssignmentResponseDTO.from(a, LIST_DESCRIPTION))
                .filter(a -> status == null || a.status() == status)
                .toList();
    }

    public AssignmentResponseDTO get(Long userId, Long id) {
        return AssignmentResponseDTO.from(owned(userId, id));
    }

    @Transactional
    public AssignmentResponseDTO setStatus(Long userId, Long id, AssignmentStatusEnum status) {
        if (status != AssignmentStatusEnum.TODO && status != AssignmentStatusEnum.SUBMITTED) {
            throw new BadRequestException("Можно поставить только todo или submitted");
        }
        AssignmentEntity a = owned(userId, id);
        if (a.getStatus() == AssignmentStatusEnum.GRADED) {
            throw new BadRequestException("Задание уже оценено учителем");
        }
        a.setStatus(status);
        assignments.save(a);
        if (status == AssignmentStatusEnum.SUBMITTED) {
            events.add(userId, "homework_done", "assignments", id);
        }
        return AssignmentResponseDTO.from(a);
    }

    private AssignmentEntity owned(Long userId, Long id) {
        return assignments.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Задание не найдено"));
    }
}
