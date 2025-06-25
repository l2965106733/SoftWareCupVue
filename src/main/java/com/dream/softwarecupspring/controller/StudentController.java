package com.dream.softwarecupspring.controller;

import com.dream.softwarecupspring.pojo.*;
import com.dream.softwarecupspring.service.StudentService;
import com.dream.softwarecupspring.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    // ==================== 学习相关接口 ====================
    
    /**
     * 获取学生的课件列表
     */
    @GetMapping("/courseware/{studentId}")
    public Result getCoursewareList(@PathVariable Integer studentId) {
        try {
            List<Map<String, Object>> coursewareList = studentService.getCoursewareList(studentId);
            return Result.success(coursewareList);
        } catch (Exception e) {
            return Result.error("获取课件列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取学生学习统计数据
     */
    @GetMapping("/stats/{studentId}")
    public Result getStudyStats(@PathVariable Integer studentId) {
        try {
            Map<String, Object> stats = studentService.getStudyStats(studentId);
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("获取学习统计失败：" + e.getMessage());
        }
    }

    /**
     * 记录学生查看课件行为
     */
    @PostMapping("/studyRecord")
    public Result recordStudyBehavior(@RequestBody StudyRecord studyRecord) {
        try {
            studentService.recordStudyBehavior(studyRecord);
            return Result.success("学习记录保存成功");
        } catch (Exception e) {
            return Result.error("保存学习记录失败：" + e.getMessage());
        }
    }

    /**
     * 记录AI提问
     */
    @PostMapping("/aiQuestion")
    public Result recordAiQuestion(@RequestBody AiQuestion aiQuestion) {
        try {
            studentService.recordAiQuestion(aiQuestion);
            return Result.success("AI提问记录成功");
        } catch (Exception e) {
            return Result.error("记录AI提问失败：" + e.getMessage());
        }
    }

    // ==================== 作业相关接口 ====================

    /**
     * 获取学生作业列表
     */
    @GetMapping("/homework/{studentId}")
    public Result getHomeworkList(@PathVariable Integer studentId) {
        try {
            List<Map<String, Object>> homeworkList = studentService.getHomeworkList(studentId);
            return Result.success(homeworkList);
        } catch (Exception e) {
            return Result.error("获取作业列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取作业详情
     */
    @GetMapping("/homeworkDetail/{homeworkId}")
    public Result getHomeworkDetail(@PathVariable Integer homeworkId) {
        try {
            Map<String, Object> homeworkDetail = studentService.getHomeworkDetail(homeworkId);
            return Result.success(homeworkDetail);
        } catch (Exception e) {
            return Result.error("获取作业详情失败：" + e.getMessage());
        }
    }

    /**
     * 保存作业草稿
     */
    @PostMapping("/homeworkDraft")
    public Result saveHomeworkDraft(@RequestBody StudentHomework studentHomework) {
        try {
            studentService.saveHomeworkDraft(studentHomework);
            return Result.success("草稿保存成功");
        } catch (Exception e) {
            return Result.error("保存草稿失败：" + e.getMessage());
        }
    }

    /**
     * 提交作业
     */
    @PostMapping("/homeworkSubmit")
    public Result submitHomework(@RequestBody StudentHomework studentHomework) {
        try {
            studentService.submitHomework(studentHomework);
            return Result.success("作业提交成功");
        } catch (Exception e) {
            return Result.error("提交作业失败：" + e.getMessage());
        }
    }

    /**
     * 获取学生作业统计
     */
    @GetMapping("/homeworkStats/{studentId}")
    public Result getHomeworkStats(@PathVariable Integer studentId) {
        try {
            Map<String, Object> stats = studentService.getHomeworkStats(studentId);
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("获取作业统计失败：" + e.getMessage());
        }
    }

    // ==================== 互动问答相关接口 ====================

    /**
     * 提交问题
     */
    @PostMapping("/questionSubmit")
    public Result submitQuestion(@RequestBody StudentQuestion studentQuestion) {
        try {
            studentService.submitQuestion(studentQuestion);
            return Result.success("问题提交成功");
        } catch (Exception e) {
            return Result.error("提交问题失败：" + e.getMessage());
        }
    }

    /**
     * 获取我的问题列表
     */
    @GetMapping("/questions/{studentId}")
    public Result getMyQuestions(@PathVariable Integer studentId) {
        try {
            List<StudentQuestion> questions = studentService.getMyQuestions(studentId);
            return Result.success(questions);
        } catch (Exception e) {
            return Result.error("获取问题列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取问题详情
     */
    @GetMapping("/questionDetail/{questionId}")
    public Result getQuestionDetail(@PathVariable Integer questionId) {
        try {
            StudentQuestion question = studentService.getQuestionDetail(questionId);
            return Result.success(question);
        } catch (Exception e) {
            return Result.error("获取问题详情失败：" + e.getMessage());
        }
    }

    /**
     * 评价回答
     */
    @PostMapping("/questionRate")
    public Result rateAnswer(@RequestBody Map<String, Object> rateData) {
        try {
            Integer questionId = (Integer) rateData.get("questionId");
            Integer rating = (Integer) rateData.get("rating");
            studentService.rateAnswer(questionId, rating);
            return Result.success("评价成功");
        } catch (Exception e) {
            return Result.error("评价失败：" + e.getMessage());
        }
    }

    /**
     * 获取互动统计
     */
    @GetMapping("/interactStats/{studentId}")
    public Result getInteractStats(@PathVariable Integer studentId) {
        try {
            Map<String, Object> stats = studentService.getInteractStats(studentId);
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("获取互动统计失败：" + e.getMessage());
        }
    }
} 