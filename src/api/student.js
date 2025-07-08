import request from "@/utils/request";

// ==================== AI聊天接口 ====================
export const getChatApi = (data) => request.post("/student/chat", data);

// ==================== 学习模块相关接口 ====================

// 获取学生的课件列表（基于student_teacher关系）
export const getCoursewareListApi = (studentId) => request.get(`/student/courseware/${studentId}`);

// 获取学生学习统计数据（支持参数）
export const getStudyStatsApi = (studentId, params = {}) => request.get(`/student/stats/${studentId}`, { params });

// 记录学生学习行为（增强版）
export const recordStudyBehaviorApi = (data) => request.post("/student/studyRecord", data);

// 记录AI提问（增强版）
export const recordAiQuestionApi = (data) => request.post("/student/aiQuestion", data);

// 获取学生学习详细记录
export const getStudyRecordsApi = (studentId, params = {}) => request.get(`/student/studyRecords/${studentId}`, { params });

// 获取学习进度详情
export const getStudyProgressApi = (studentId, resourceId) => request.get(`/student/studyProgress/${studentId}/${resourceId}`);

// 更新学习进度
export const updateStudyProgressApi = (data) => request.post("/student/updateProgress", data);

// 导出学习报告
export const exportStudyReportApi = (studentId, params = {}) => request.get(`/student/exportReport/${studentId}`, { params });

// ==================== 作业相关接口 ====================

export const getHomeworkListApi = (studentId) => request.get(`/student/homework/${studentId}`);

export const getHomeworkDetailApi = (homeworkId) => request.get(`/student/homeworkDetail/${homeworkId}`);

export const saveHomeworkDraftApi = (data) => request.post("/student/homeworkDraft", data);

export const submitHomeworkApi = (data) => request.post("/student/homeworkSubmit", data);

export const getHomeworkStatsApi = (studentId) => request.get(`/student/homeworkStats/${studentId}`);

// ==================== 互动问答相关接口 ====================

export const submitQuestionApi = (data) => request.post("/student/questionSubmit", data);

export const getMyQuestionsApi = (studentId) => request.get(`/student/questions/${studentId}`);

export const getQuestionDetailApi = (questionId) => request.get(`/student/questionDetail/${questionId}`);

// 评价教师回答
export const rateAnswerApi = (data) => request.post("/student/questionRate", data);

export const getInteractStatsApi = (studentId) => request.get(`/student/interactStats/${studentId}`);

export const getTeacherIdApi = (studentId) => request.get(`/student/teacherId/${studentId}`);

// ==================== 评分系统相关接口 ====================

// 提交评分（只包含rating分数）
export const submitRatingApi = (questionId, rating) => request.post("/student/rating/submit", { questionId, rating });

// 获取特定问题的评分
export const getRatingApi = (questionId) => request.get(`/student/rating/${questionId}`);

// 获取学生的评分历史
export const getRatingHistoryApi = (studentId) => request.get(`/student/rating/history/${studentId}`);

// 获取学生评分统计
export const getRatingStatsApi = (studentId) => request.get(`/student/rating/stats/${studentId}`);

// ==================== AI问答增强接口 ====================

// 获取AI问答历史
export const getAiQuestionHistoryApi = (studentId, params = {}) => request.get(`/student/aiQuestions/${studentId}`, { params });

// 评价AI回答满意度
export const rateAiAnswerApi = (data) => request.post("/student/aiQuestion/rate", data);

// 获取AI问答统计
export const getAiQuestionStatsApi = (studentId) => request.get(`/student/aiQuestion/stats/${studentId}`);