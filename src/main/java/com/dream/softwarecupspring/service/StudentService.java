package com.dream.softwarecupspring.service;

import com.dream.softwarecupspring.pojo.*;
import java.util.List;
import java.util.Map;

public interface StudentService {

    // ==================== 学习相关方法 ====================
    
    /**
     * 获取学生的课件列表
     */
    List<Map<String, Object>> getCoursewareList(Integer studentId);

    /**
     * 获取学生学习统计数据
     */
    Map<String, Object> getStudyStats(Integer studentId);

    /**
     * 记录学生查看课件行为
     */
    void recordStudyBehavior(StudyRecord studyRecord);

    /**
     * 记录AI提问
     */
    void recordAiQuestion(AiQuestion aiQuestion);

    // ==================== 作业相关方法 ====================

    /**
     * 获取学生作业列表
     */
    List<Map<String, Object>> getHomeworkList(Integer studentId);

    /**
     * 获取作业详情
     */
    Map<String, Object> getHomeworkDetail(Integer homeworkId);

    /**
     * 保存作业草稿
     */
    void saveHomeworkDraft(StudentHomework studentHomework);

    /**
     * 提交作业
     */
    void submitHomework(StudentHomework studentHomework);

    /**
     * 获取学生作业统计
     */
    Map<String, Object> getHomeworkStats(Integer studentId);

    // ==================== 互动问答相关方法 ====================

    /**
     * 提交问题
     */
    void submitQuestion(StudentQuestion studentQuestion);

    /**
     * 获取我的问题列表
     */
    List<StudentQuestion> getMyQuestions(Integer studentId);

    /**
     * 获取问题详情
     */
    StudentQuestion getQuestionDetail(Integer questionId);

    /**
     * 评价回答
     */
    void rateAnswer(Integer questionId, Integer rating);

    /**
     * 获取互动统计
     */
    Map<String, Object> getInteractStats(Integer studentId);

    // ==================== 评分系统相关方法 ====================

    /**
     * 提交评分（只包含rating分数）
     */
    void submitRating(Integer questionId, Integer rating);

    /**
     * 获取特定问题的评分
     */
    Integer getRating(Integer questionId);

    /**
     * 获取学生的评分历史
     */
    List<StudentQuestion> getRatingHistory(Integer studentId);

    /**
     * 获取学生评分统计
     */
    Map<String, Object> getRatingStats(Integer studentId);
} 