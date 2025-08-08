import request from "../utils/request";

export const pageQueryApi = (name,gender,role,subject,page,pageSize) => request.get(`/admin/user?name=${name}&gender=${gender}&role=${role}&subject=${subject}&page=${page}&pageSize=${pageSize}`);

export const deleteByIdApi = (ids) => request.delete('/admin/user', {params: { ids }});

export const getUserInfoApi = (id) => request.get(`/admin/user/${id}`);

export const updateUserApi = (user) => request.put("/admin/user",user)

export const addUserApi = (user) => request.post("/admin/user",user)

export const sumbitSelectStudentsApi = (selectedStudentIds,teacherId) => request.put(`/admin/user/submitSelectedStudents/${teacherId}`,selectedStudentIds)

export const getAllStudentsApi = (teacherId) => request.get(`/admin/user/getAllStudents/${teacherId}`);
// 资源管理相关API
export const getAllResourcesApi = () => request.get("/admin/resource");

export const deleteResourceApi = (resourceId) => request.delete(`/admin/resource/${resourceId}`);

export const updateResourceApi = (data) => request.put("/admin/resource/update", data);

export const getResourceStatsApi = () => request.get("/admin/resource/stats");

// ==================== 系统总体统计相关API ====================

// 1. 获取系统概览数据
export const getSystemOverviewApi = () => request.get("/admin/systemOverview");

// 2. 获取用户活跃度数据
export const getUserActivityApi = () => request.get("/admin/userActivity");

// 5. 获取用户活跃度趋势
export const getUserActivityTrendApi = (params) => request.get('/admin/userActivityTrend', { params });

export const getTopKnowledgeScoreApi = () => request.get("/admin/topKnowledgeScore");

export const getKnowledgeDistributionApi = (params) => request.get("/admin/knowledgeDistribution",{ params })

// ==================== 管理员首页专用接口 ====================

// 获取最近活动记录（管理员首页专用）
export const getRecentActivitiesApi = () => request.get("/admin/recentActivities");



