import request from "@/utils/request";

export const getTeachingPlanApi = (remark, fileUrls) => request.post("/teacher/teachingPlan", {remark,fileUrls});

export const getQuestionApi = (data) => request.post("/teacher/question", data);

export const getAnswerApi = (data) => request.post("/teacher/answer", data);

export const saveQuestionApi = (data) => request.post("/teacher/questions", data);

export const publishHomeworkApi = (data) => request.post("/teacher/publish", data);

export const getHomeworkListApi = (teacherId) => request.get(`/teacher/homework/${teacherId}`);

export const getStudentQuestionsApi = (teacherId) => request.get(`/teacher/studentQuestion/${teacherId}`);

export const sendStudentAnswerApi = (data) => request.post("/teacher/studentAnswer", data);

export const getHomeworkDetailApi = (homeworkId) => request.get(`/teacher/detail/${homeworkId}`);

export const getStudentSubmissionsApi = (homeworkId) => request.get(`/teacher/submissions/${homeworkId}`);

export const gradeHomeworkApi = (data) => request.post("/teacher/homeworkGrade", data);

export const getHomeworkStatsApi = (teacherId) => request.get(`/teacher/homeworkStats/${teacherId}`);

export const uploadResourceApi = (data) => request.post("/teacher/resourceUpload", data);

export const getResourceListApi = (teacherId) => request.get(`/teacher/resource/${teacherId}`);

export const deleteResourceApi = (resourceId) => request.delete(`/teacher/resource/${resourceId}`);

export const updateResourceApi = (data) => request.put("/teacher/resourceUpdate", data);   