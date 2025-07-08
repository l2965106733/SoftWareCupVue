package com.dream.softwarecupspring.mapper;

import com.dream.softwarecupspring.pojo.StudentQuestion;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface StudentQuestionMapper {

    // ==================== 基本CRUD方法 ====================
    
    /**
     * 插入学生问题
     */
    void insert(StudentQuestion studentQuestion);
    
    /**
     * 根据ID更新学生问题
     */
    void updateById(StudentQuestion studentQuestion);
    
    /**
     * 根据ID查询学生问题
     */
    StudentQuestion selectById(Integer id);
    
    /**
     * 根据学生ID获取问题列表
     */
    List<StudentQuestion> getQuestionsByStudentId(Integer studentId);

    // ==================== 互动问答统计方法 ====================
    
    /**
     * 获取学生总提问数
     */
    Integer getTotalQuestionsCount(Integer studentId);
    
    /**
     * 获取学生已回答问题数
     */
    Integer getAnsweredQuestionsCount(Integer studentId);
    
    /**
     * 获取学生待回答问题数
     */
    Integer getPendingQuestionsCount(Integer studentId);
    
    /**
     * 获取学生问题平均评分
     */
    Double getAverageRating(Integer studentId);
    
    /**
     * 按类型统计问题数
     */
    List<Map<String, Object>> getQuestionsByType(Integer studentId);
    
    /**
     * 获取最近问题
     */
    List<StudentQuestion> getRecentQuestions(Integer studentId);

    // ==================== 评分系统相关方法 ====================
    
    /**
     * 获取学生所有已评分的问题
     */
    List<StudentQuestion> getRatedQuestionsByStudentId(Integer studentId);
    
    /**
     * 获取学生总评分数
     */
    Integer getTotalRatingsCount(Integer studentId);
    
    /**
     * 获取学生平均评分
     */
    Double getAverageRatingByStudent(Integer studentId);
    
    /**
     * 获取学生评分分布
     */
    List<Map<String, Object>> getRatingDistribution(Integer studentId);
    
    /**
     * 获取学生最近评分的问题
     */
    List<StudentQuestion> getRecentRatedQuestions(Integer studentId);
} 