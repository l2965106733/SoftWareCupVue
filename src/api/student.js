import request from "@/utils/request";

export const getChatApi = (data) => request.post("/student/chat", data);

// 获取学生的课件列表（基于student_teacher关系）
export const getCoursewareListApi = (studentId) => request.get(`/student/courseware/${studentId}`);

// 获取学生学习统计数据
export const getStudyStatsApi = (studentId) => request.get(`/student/stats/${studentId}`);

// 记录学生查看课件行为
export const recordStudyBehaviorApi = (data) => request.post("/student/studyRecord", data);

// 记录AI提问
export const recordAiQuestionApi = (data) => request.post("/student/aiQuestion", data);

// 作业相关接口
export const getHomeworkListApi = (studentId) => request.get(`/student/homework/${studentId}`);

export const getHomeworkDetailApi = (homeworkId) => request.get(`/student/homeworkDetail/${homeworkId}`);

export const saveHomeworkDraftApi = (data) => request.post("/student/homeworkDraft", data);

export const submitHomeworkApi = (data) => request.post("/student/homeworkSubmit", data);

export const getHomeworkStatsApi = (studentId) => request.get(`/student/homeworkStats/${studentId}`);

// 互动问答相关接口
export const submitQuestionApi = (data) => request.post("/student/questionSubmit", data);

export const getMyQuestionsApi = (studentId) => request.get(`/student/questions/${studentId}`);

export const getQuestionDetailApi = (questionId) => request.get(`/student/questionDetail/${questionId}`);

export const rateAnswerApi = (data) => request.post("/student/questionRate", data);

export const getInteractStatsApi = (studentId) => request.get(`/student/interactStats/${studentId}`);

export const getTeacherIdApi = (studentId) => request.get(`/student/teacherId/${studentId}`);