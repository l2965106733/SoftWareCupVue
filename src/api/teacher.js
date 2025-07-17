import request from "@/utils/request";

export const getTeacherHomeOverviewApi = (teacherId) => request.get(`/teacher/stats/homeOverview/${teacherId}`);

export const getTeacherActivitiesApi = (teacherId, limit = 10) => request.get(`/teacher/stats/activities/${teacherId}?limit=${limit}`);

export const getQuestionApi = (data) => request.post("/teacher/homework/aiQuestion", data);

export const getAnswerApi = (data) => request.post("/teacher/interact/aiAnswer", data);

export const saveQuestionApi = (data) => request.post("/teacher/homework/questions", data);

export const publishHomeworkApi = (data) => request.post("/teacher/homework/publish", data);

export const getHomeworkListApi = (teacherId) => request.get(`/teacher/homework/${teacherId}`);

export const getStudentQuestionsApi = (teacherId) => request.get(`/teacher/interact/studentQuestion/${teacherId}`);

export const sendStudentAnswerApi = (data) => request.post("/teacher/interact/studentAnswer", data);

export const getHomeworkDetailApi = (homeworkId) => request.get(`/teacher/homework/detail/${homeworkId}`);

export const getHomeworkDetailWithAnswerApi = (homeworkId, studentId) =>
  request.get(`/teacher/homework/detailWithAnswer`, { params: { homeworkId, studentId } });

export const getStudentSubmissionsApi = (homeworkId) => request.get(`/teacher/homework/submissions/${homeworkId}`);

export const gradeHomeworkApi = (data) => request.post("/teacher/homework/grade", data);

export const getHomeworkStatsApi = (teacherId) => request.get(`/teacher/stats/homeworkStats/${teacherId}`);

export const getTeacherOverviewApi = (teacherId) => request.get(`/teacher/stats/overview/${teacherId}`);
export const getStudentListApi = (teacherId) => request.get(`/teacher/stats/students/${teacherId}`);
export const getResourceStatsApi = (teacherId) => request.get(`/teacher/stats/resourceStats/${teacherId}`);
export const getInteractStatsApi = (teacherId) => request.get(`/teacher/stats/interactStats/${teacherId}`);
export const uploadResourceApi = (data) => request.post("/teacher/resource/upload", data);

export const getResourceListApi = (teacherId) => request.get(`/teacher/resource/${teacherId}`);

export const deleteResourceApi = (resourceId) => request.delete(`/teacher/resource/${resourceId}`);

export const updateResourceApi = (data) => request.put("/teacher/resource/update", data);   

export const getTeachingPlanApi = (remark, fileUrls) => request.post("/teacher/resource/aiTeachingPlan", {remark,fileUrls});


export const getStudentRatingsApi = (teacherId) => request.get(`/teacher/interact/ratings/${teacherId}`);
