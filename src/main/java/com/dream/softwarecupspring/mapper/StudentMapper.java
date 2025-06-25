package com.dream.softwarecupspring.mapper;

import com.dream.softwarecupspring.pojo.StudentHomework;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface StudentMapper {

    // ==================== 学习相关方法 ====================
    
    /**
     * 根据学生ID获取课件列表
     */
    List<Map<String, Object>> getCoursewareListByStudentId(Integer studentId);

    // ==================== 作业相关方法 ====================
    
    /**
     * 根据学生ID获取作业列表
     */
    List<Map<String, Object>> getHomeworkListByStudentId(Integer studentId);
    
    /**
     * 根据作业ID获取作业详情
     */
    Map<String, Object> getHomeworkDetailById(Integer homeworkId);
    
    /**
     * 根据作业ID和学生ID查询学生作业记录
     */
    StudentHomework getByHomeworkAndStudent(Integer homeworkId, Integer studentId);
    
    /**
     * 插入学生作业记录
     */
    void insert(StudentHomework studentHomework);
    
    /**
     * 更新学生作业记录
     */
    void updateById(StudentHomework studentHomework);
    
    // ==================== 统计相关方法 ====================
    
    /**
     * 获取学生总作业数
     */
    Integer getTotalHomeworkCount(Integer studentId);
    
    /**
     * 获取学生已提交作业数
     */
    Integer getSubmittedHomeworkCount(Integer studentId);
    
    /**
     * 获取学生已批改作业数
     */
    Integer getGradedHomeworkCount(Integer studentId);
    
    /**
     * 获取学生平均分
     */
    Double getAverageScore(Integer studentId);
    
    /**
     * 获取学生最高分
     */
    Integer getBestScore(Integer studentId);
    
    /**
     * 获取学生最近作业
     */
    List<Map<String, Object>> getRecentHomework(Integer studentId);
    
    /**
     * 获取学生成绩分布
     */
    List<Map<String, Object>> getScoreDistribution(Integer studentId);
} 