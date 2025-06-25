package com.dream.softwarecupspring.service.impl;

import com.dream.softwarecupspring.mapper.*;
import com.dream.softwarecupspring.pojo.*;
import com.dream.softwarecupspring.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentMapper studentMapper;
    
    @Autowired
    private StudyRecordMapper studyRecordMapper;
    
    @Autowired
    private AiQuestionMapper aiQuestionMapper;
    
    @Autowired
    private StudentHomeworkMapper studentHomeworkMapper;
    
    @Autowired
    private StudentQuestionMapper studentQuestionMapper;

    // ==================== 学习相关方法 ====================

    @Override
    public List<Map<String, Object>> getCoursewareList(Integer studentId) {
        // 根据学生-教师关系获取课件列表
        return studentMapper.getCoursewareListByStudentId(studentId);
    }

    @Override
    public Map<String, Object> getStudyStats(Integer studentId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 获取学习统计数据
        stats.put("totalStudyTime", studyRecordMapper.getTotalStudyTime(studentId));           // 总学习时长
        stats.put("todayStudyTime", studyRecordMapper.getTodayStudyTime(studentId));           // 今日学习时长
        stats.put("weekStudyTime", studyRecordMapper.getWeekStudyTime(studentId));             // 本周学习时长
        stats.put("completedCourseware", studyRecordMapper.getCompletedCoursewareCount(studentId)); // 完成课件数
        stats.put("totalCourseware", studyRecordMapper.getTotalCoursewareCount(studentId));    // 总课件数
        stats.put("avgProgress", studyRecordMapper.getAverageProgress(studentId));             // 平均进度
        stats.put("studyDays", studyRecordMapper.getStudyDaysCount(studentId));                // 学习天数
        stats.put("recentStudyRecords", studyRecordMapper.getRecentStudyRecords(studentId));   // 最近学习记录
        
        return stats;
    }

    @Override
    public void recordStudyBehavior(StudyRecord studyRecord) {
        studyRecord.setCreatedTime(LocalDateTime.now());
        studyRecord.setUpdatedTime(LocalDateTime.now());
        studyRecordMapper.insert(studyRecord);
    }

    @Override
    public void recordAiQuestion(AiQuestion aiQuestion) {
        aiQuestion.setCreatedTime(LocalDateTime.now());
        aiQuestion.setUpdatedTime(LocalDateTime.now());
        aiQuestionMapper.insert(aiQuestion);
    }

    // ==================== 作业相关方法 ====================

    @Override
    public List<Map<String, Object>> getHomeworkList(Integer studentId) {
        // 获取学生的作业列表，包含作业信息和提交状态
        return studentHomeworkMapper.getHomeworkListByStudentId(studentId);
    }

    @Override
    public Map<String, Object> getHomeworkDetail(Integer homeworkId) {
        // 获取作业详情，包含题目列表
        return studentHomeworkMapper.getHomeworkDetailById(homeworkId);
    }

    @Override
    public void saveHomeworkDraft(StudentHomework studentHomework) {
        // 保存草稿状态
        studentHomework.setStatus(0); // 0: 草稿状态
        studentHomework.setUpdatedTime(LocalDateTime.now());
        
        // 检查是否已存在记录
        StudentHomework existing = studentHomeworkMapper.getByHomeworkAndStudent(
            studentHomework.getHomeworkId(), studentHomework.getStudentId());
        
        if (existing != null) {
            studentHomework.setId(existing.getId());
            studentHomeworkMapper.updateById(studentHomework);
        } else {
            studentHomework.setCreatedTime(LocalDateTime.now());
            studentHomeworkMapper.insert(studentHomework);
        }
        
        // 保存答题记录
        saveStudentAnswers(studentHomework);
    }

    @Override
    public void submitHomework(StudentHomework studentHomework) {
        // 提交作业
        studentHomework.setStatus(1); // 1: 已提交
        studentHomework.setSubmitTime(LocalDateTime.now());
        studentHomework.setUpdatedTime(LocalDateTime.now());
        
        // 检查是否已存在记录
        StudentHomework existing = studentHomeworkMapper.getByHomeworkAndStudent(
            studentHomework.getHomeworkId(), studentHomework.getStudentId());
        
        if (existing != null) {
            studentHomework.setId(existing.getId());
            studentHomeworkMapper.updateById(studentHomework);
        } else {
            studentHomework.setCreatedTime(LocalDateTime.now());
            studentHomeworkMapper.insert(studentHomework);
        }
        
        // 保存答题记录
        saveStudentAnswers(studentHomework);
    }

    @Override
    public Map<String, Object> getHomeworkStats(Integer studentId) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalHomework", studentHomeworkMapper.getTotalHomeworkCount(studentId));     // 总作业数
        stats.put("submittedHomework", studentHomeworkMapper.getSubmittedHomeworkCount(studentId)); // 已提交作业数
        stats.put("gradedHomework", studentHomeworkMapper.getGradedHomeworkCount(studentId));   // 已批改作业数
        stats.put("avgScore", studentHomeworkMapper.getAverageScore(studentId));                // 平均分
        stats.put("bestScore", studentHomeworkMapper.getBestScore(studentId));                  // 最高分
        stats.put("recentHomework", studentHomeworkMapper.getRecentHomework(studentId));        // 最近作业
        stats.put("scoreDistribution", studentHomeworkMapper.getScoreDistribution(studentId));  // 成绩分布
        
        return stats;
    }

    // ==================== 互动问答相关方法 ====================

    @Override
    public void submitQuestion(StudentQuestion studentQuestion) {
        studentQuestion.setStatus(0); // 0: 待回答
        studentQuestion.setCreatedTime(LocalDateTime.now());
        studentQuestion.setUpdatedTime(LocalDateTime.now());
        studentQuestionMapper.insert(studentQuestion);
    }

    @Override
    public List<StudentQuestion> getMyQuestions(Integer studentId) {
        return studentQuestionMapper.getQuestionsByStudentId(studentId);
    }

    @Override
    public StudentQuestion getQuestionDetail(Integer questionId) {
        return studentQuestionMapper.selectById(questionId);
    }

    @Override
    public void rateAnswer(Integer questionId, Integer rating) {
        StudentQuestion question = new StudentQuestion();
        question.setId(questionId);
        question.setRating(rating);
        question.setUpdatedTime(LocalDateTime.now());
        studentQuestionMapper.updateById(question);
    }

    @Override
    public Map<String, Object> getInteractStats(Integer studentId) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalQuestions", studentQuestionMapper.getTotalQuestionsCount(studentId));   // 总提问数
        stats.put("answeredQuestions", studentQuestionMapper.getAnsweredQuestionsCount(studentId)); // 已回答数
        stats.put("pendingQuestions", studentQuestionMapper.getPendingQuestionsCount(studentId)); // 待回答数
        stats.put("avgRating", studentQuestionMapper.getAverageRating(studentId));               // 平均评分
        stats.put("questionsByType", studentQuestionMapper.getQuestionsByType(studentId));       // 按类型统计
        stats.put("recentQuestions", studentQuestionMapper.getRecentQuestions(studentId));       // 最近问题
        
        return stats;
    }

    // ==================== 私有辅助方法 ====================

    private void saveStudentAnswers(StudentHomework studentHomework) {
        // 这里需要根据前端传递的答案数据保存到student_answers表
        // 具体实现根据你的数据结构来定
        if (studentHomework.getScores() != null) {
            for (Map.Entry<String, Integer> entry : studentHomework.getScores().entrySet()) {
                // 保存学生答案逻辑
                // studentAnswerMapper.saveOrUpdate(studentAnswer);
            }
        }
    }
} 