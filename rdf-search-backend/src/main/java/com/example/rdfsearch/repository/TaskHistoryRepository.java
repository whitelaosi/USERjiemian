package com.example.rdfsearch.repository;

import com.example.rdfsearch.model.TaskHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface TaskHistoryRepository extends JpaRepository<TaskHistory, Long> {
    // 按用户ID查询历史记录
    List<TaskHistory> findByUserId(String userId, Pageable pageable);

    // 按任务URI查询历史记录
    List<TaskHistory> findByTaskUriOrderByQueryTimeDesc(String taskUri);

    // 删除某日期之前的记录
    void deleteByQueryTimeBefore(Date date);

    // 删除某用户的所有记录
    void deleteByUserId(String userId);

    // 删除某用户在某日期之前的记录
    void deleteByUserIdAndQueryTimeBefore(String userId, Date date);
}