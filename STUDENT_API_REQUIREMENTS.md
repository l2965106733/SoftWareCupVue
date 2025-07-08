# 学生端API后端接口需求文档

## 📋 接口总览

本文档详细说明前端调用的所有学生端API接口，后端需要按照这些规范实现对应的接口。

---

## 🔧 基础功能接口

### 1. AI聊天接口
- **URL**: `POST /student/chat`
- **请求体**: 
  ```json
  {
    "message": "你好，我想学习Java",
    "studentId": 1
  }
  ```
- **响应格式**: 
  ```json
  {
    "code": 1,
    "msg": "success",
    "data": {
      "reply": "你好！我可以帮助你学习Java编程..."
    }
  }
  ```

### 2. 获取学生分配的教师ID
- **URL**: `GET /student/teacherId/{studentId}`
- **响应格式**: 
  ```json
  {
    "code": 1,
    "msg": "success", 
    "data": 2
  }
  ```

---

## 📚 学习模块接口

### 3. 获取学生课件列表
- **URL**: `GET /student/courseware/{studentId}`
- **响应格式**: 
  ```json
  {
    "code": 1,
    "data": [
      {
        "id": 1,
        "title": "Java基础语法",
        "description": "Java基础知识介绍",
        "url": "/courseware/java-basic.pdf",
        "progress": 80,
        "completed": false
      }
    ]
  }
  ```

### 4. 获取学习统计数据
- **URL**: `GET /student/stats/{studentId}`
- **响应格式**: 
  ```json
  {
    "code": 1,
    "data": {
      "totalStudyTime": 120,
      "todayStudyTime": 30,
      "weekStudyTime": 180,
      "completedCourseware": 5,
      "totalCourseware": 10,
      "avgProgress": 65.5,
      "studyDays": 15
    }
  }
  ```

### 5. 记录学习行为
- **URL**: `POST /student/studyRecord`
- **请求体**: 
  ```json
  {
    "studentId": 1,
    "coursewareId": 1,
    "studyTime": 30,
    "progress": 75
  }
  ```

### 6. 记录AI提问
- **URL**: `POST /student/aiQuestion`
- **请求体**: 
  ```json
  {
    "studentId": 1,
    "question": "什么是多态？",
    "answer": "多态是面向对象编程的重要特性...",
    "type": "java"
  }
  ```

---

## 📝 作业模块接口

### 7. 获取作业列表
- **URL**: `GET /student/homework/{studentId}`
- **响应格式**: 
  ```json
  {
    "code": 1,
    "data": [
      {
        "id": 1,
        "title": "Java基础练习",
        "description": "完成Java基础语法练习",
        "deadline": "2024-01-20 23:59:59",
        "status": "pending",
        "totalScore": 100,
        "teacherName": "张老师"
      }
    ]
  }
  ```

### 8. 获取作业详情
- **URL**: `GET /student/homeworkDetail/{homeworkId}`
- **响应格式**: 
  ```json
  {
    "code": 1,
    "data": {
      "id": 1,
      "title": "Java基础练习",
      "description": "完成以下题目",
      "questions": [
        {
          "id": 1,
          "content": "请解释Java中的变量类型",
          "type": "short",
          "score": 20
        }
      ]
    }
  }
  ```

### 9. 保存作业草稿
- **URL**: `POST /student/homeworkDraft`
- **请求体**: 
  ```json
  {
    "studentId": 1,
    "homeworkId": 1,
    "answers": {
      "1": "Java有基本数据类型和引用数据类型..."
    },
    "status": "draft"
  }
  ```

### 10. 提交作业
- **URL**: `POST /student/homeworkSubmit`
- **请求体**: 
  ```json
  {
    "studentId": 1,
    "homeworkId": 1,
    "answers": {
      "1": "Java有基本数据类型和引用数据类型..."
    },
    "status": "submitted"
  }
  ```

### 11. 获取作业统计
- **URL**: `GET /student/homeworkStats/{studentId}`
- **响应格式**: 
  ```json
  {
    "code": 1,
    "data": {
      "totalHomework": 10,
      "completedHomework": 7,
      "pendingHomework": 3,
      "averageScore": 85.5,
      "overallProgress": 70
    }
  }
  ```

---

## 💬 互动问答模块接口

### 12. 提交问题
- **URL**: `POST /student/questionSubmit`
- **请求体**: 
  ```json
  {
    "studentId": 1,
    "teacherId": 2,
    "title": "关于Java继承的问题",
    "content": "我在学习Java继承时遇到了困惑...",
    "type": "java"
  }
  ```

### 13. 获取问题列表
- **URL**: `GET /student/questions/{studentId}`
- **响应格式**: 
  ```json
  {
    "code": 1,
    "data": [
      {
        "id": 1,
        "title": "关于Java继承的问题",
        "content": "我在学习Java继承时遇到了困惑...",
        "type": "java",
        "status": 1,
        "createdTime": "2024-01-15 14:30:00",
        "answeredTime": "2024-01-15 16:45:00",
        "answer": "继承是面向对象编程的重要概念...",
        "teacherName": "张老师",
        "rating": 5
      }
    ]
  }
  ```

### 14. 获取问题详情
- **URL**: `GET /student/questionDetail/{questionId}`
- **响应格式**: 
  ```json
  {
    "code": 1,
    "data": {
      "id": 1,
      "title": "关于Java继承的问题",
      "content": "详细问题内容...",
      "answer": "详细回答内容...",
      "teacherName": "张老师",
      "answeredTime": "2024-01-15 16:45:00",
      "rating": 5
    }
  }
  ```

### 15. 评价回答 ⭐ 重点接口
- **URL**: `POST /student/questionRate`
- **请求体**: 
  ```json
  {
    "questionId": 1,
    "rating": 5
  }
  ```
- **响应格式**: 
  ```json
  {
    "code": 1,
    "msg": "评价成功",
    "data": null
  }
  ```
- **后端处理**: 更新数据库中对应问题的rating字段

### 16. 获取互动统计
- **URL**: `GET /student/interactStats/{studentId}`
- **响应格式**: 
  ```json
  {
    "code": 1,
    "data": {
      "totalQuestions": 10,
      "answeredQuestions": 8,
      "pendingQuestions": 2,
      "avgRating": 4.5,
      "avgResponseTime": 120
    }
  }
  ```

---

## 🎯 补充的扩展接口

### 17. 获取学生个人信息
- **URL**: `GET /student/profile/{studentId}`

### 18. 更新个人信息
- **URL**: `PUT /student/profile`

### 19. 修改密码
- **URL**: `POST /student/changePassword`

### 20. 获取课程表
- **URL**: `GET /student/schedule/{studentId}`

### 21. 获取学习进度概览
- **URL**: `GET /student/progress/{studentId}`

### 22. 获取学习日历
- **URL**: `GET /student/calendar/{studentId}?month={month}`

### 23. 文件上传
- **URL**: `POST /student/upload`

### 24. 获取通知列表
- **URL**: `GET /student/notifications/{studentId}?page={page}&size={size}`

### 25. 标记通知已读
- **URL**: `PUT /student/notifications/{notificationId}/read`

### 26. 获取学习资源
- **URL**: `GET /student/resources/{studentId}?type={type}`

### 27. 记录学习行为
- **URL**: `POST /student/behavior`

### 28. 获取错题集
- **URL**: `GET /student/wrongQuestions/{studentId}`

### 29. 收藏问题
- **URL**: `POST /student/questionFavorite`

### 30. 获取收藏问题列表
- **URL**: `GET /student/favoriteQuestions/{studentId}`

---

## 📊 统一响应格式

所有接口都应该遵循统一的响应格式：

```json
{
  "code": 1,           // 1表示成功，0表示失败
  "msg": "success",    // 响应消息
  "data": {}          // 响应数据，失败时可为null
}
```

## 🔒 认证说明

所有接口都需要进行用户身份认证，确保只有登录的学生用户才能访问对应的接口。

## 🎯 优先级

**高优先级（核心功能）**:
- 接口 1-16：基础学习、作业、互动问答功能
- 特别是接口15（评价回答）需要重点实现

**中优先级（扩展功能）**:
- 接口 17-23：个人信息、进度、通知等

**低优先级（高级功能）**:
- 接口 24-30：高级学习功能和辅助功能 