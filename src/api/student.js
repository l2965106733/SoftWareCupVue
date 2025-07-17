import request from "@/utils/request";

// ==================== AI聊天接口 ====================
export const getChatApi = (data) => request.post("/student/study/chat", data);

// ==================== 学生首页相关接口 ====================
// 获取学生首页统计数据
export const getStudentHomeStatsApi = (studentId) => request.get(`/student/interact/homeStats/${studentId}`);

// 获取学生最近活动
export const getStudentRecentActivitiesApi = (studentId) => request.get(`/student/interact/recentActivities/${studentId}`);

// ==================== 学习模块相关接口 ====================

// 获取学生的课件列表（基于student_teacher关系）
export const getCoursewareListApi = (studentId) => request.get(`/student/study/courseware/${studentId}`);

// 获取学生学习统计数据（支持参数）
export const getStudyStatsApi = (studentId, params = {}) => request.get(`/student/study/stats/${studentId}`, { params });

// 记录学生学习行为（增强版）
export const recordStudyBehaviorApi = (data) => request.post("/student/study/record", data);

// 记录AI提问（增强版）
export const recordAiQuestionApi = (data) => request.post("/student/study/aiQuestion", data);

// 获取学生学习详细记录
export const getStudyRecordsApi = (studentId, params = {}) => request.get(`/student/study/records/${studentId}`, { params });

// 获取学习时长趋势
export const getStudyTimeTrendApi = (studentId, params = {}) => request.get(`/student/study/timeTrend/${studentId}`, { params });

// ==================== 作业相关接口 ====================

export const getHomeworkListApi = (studentId) => request.get(`/student/homework/${studentId}`);

export const getHomeworkDetailApi = (homeworkId) => request.get(`/student/homework/detail/${homeworkId}`);

export const saveHomeworkDraftApi = (data) => request.post("/student/homework/draft", data);

export const submitHomeworkApi = (data) => request.post("/student/homework/submit", data);

export const getHomeworkStatsApi = (studentId) => request.get(`/student/homework/stats/${studentId}`);

export const getAnalysisApi = (data) => request.post("/student/homework/analysis", data);
// ==================== 互动问答相关接口 ====================

export const submitQuestionApi = (data) => request.post("/student/interact/questionSubmit", data);

export const getMyQuestionsApi = (studentId) => request.get(`/student/interact/questions/${studentId}`);

export const getQuestionDetailApi = (questionId) => request.get(`/student/interact/questionDetail/${questionId}`);

// 评价教师回答
export const rateAnswerApi = (data) => request.post("/student/interact/questionRate", data);

export const getQuestionApi = (data) => request.post("/student/study/generateAIQuestion", data);

export const getInteractStatsApi = (studentId) => request.get(`/student/interact/stats/${studentId}`);

export const getTeacherIdApi = (studentId) => request.get(`/student/interact/teacherId/${studentId}`);

export const submitRatingApi = (questionId, rating) => request.post("/student/interact/rating/submit", { questionId, rating });

// 获取特定问题的评分
export const getRatingApi = (questionId) => request.get(`/student/interact/rating/${questionId}`);

// 获取学生的评分历史
export const getRatingHistoryApi = (studentId) => request.get(`/student/interact/rating/history/${studentId}`);

// 获取学生评分统计
export const getRatingStatsApi = (studentId) => request.get(`/student/interact/rating/stats/${studentId}`);

// ==================== AI问答增强接口 ====================

// 获取AI问答历史
export const getAiQuestionHistoryApi = (studentId, params = {}) => request.get(`/student/study/aiQuestions/${studentId}`, { params });
