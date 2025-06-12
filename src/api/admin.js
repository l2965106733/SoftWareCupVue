import request from "../utils/request";

export const pageQueryApi = (name,gender,role,subject,page,pageSize) => request.get(`/admin/user?name=${name}&gender=${gender}&role=${role}&subject=${subject}&page=${page}&pageSize=${pageSize}`);

export const deleteByIdApi = (ids) => request.delete(`/admin/user?id=${ids}`);

export const getUserInfoApi = (id) => request.get(`/admin/user/${id}`);

export const updateUserApi = (user) => request.put("/admin/user",user)

export const addUserApi = (user) => request.post("/admin/user",user)

