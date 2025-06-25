# 学生作业模块数据库设计

## 核心表结构

### 1. 作业表 (homework)
```sql
CREATE TABLE homework (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL COMMENT '发布教师ID',
    title VARCHAR(255) NOT NULL COMMENT '作业标题',
    description TEXT COMMENT '作业描述',
    total_score INT DEFAULT 0 COMMENT '总分',
    deadline DATETIME NOT NULL COMMENT '截止时间',
    status TINYINT DEFAULT 1 COMMENT '状态：1-发布中，0-已结束',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_deadline (deadline),
    INDEX idx_status (status)
);
```

### 2. 作业题目表 (homework_questions)
```sql
CREATE TABLE homework_questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    homework_id BIGINT NOT NULL COMMENT '作业ID',
    content TEXT NOT NULL COMMENT '题目内容',
    type VARCHAR(20) NOT NULL COMMENT '题型：choice-选择题，short-简答题，code-编程题',
    score INT NOT NULL COMMENT '分值',
    answer TEXT COMMENT '标准答案',
    explain TEXT COMMENT '答案解析',
    options JSON COMMENT '选择题选项（JSON格式）',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_homework_id (homework_id),
    INDEX idx_type (type),
    FOREIGN KEY (homework_id) REFERENCES homework(id) ON DELETE CASCADE
);
```

### 3. 学生作业提交表 (student_homework)
```sql
CREATE TABLE student_homework (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL COMMENT '学生ID',
    homework_id BIGINT NOT NULL COMMENT '作业ID',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending-待完成，draft-草稿，submitted-已提交，graded-已批改',
    total_score INT DEFAULT 0 COMMENT '总得分',
    feedback TEXT COMMENT '教师反馈',
    submit_time DATETIME COMMENT '提交时间',
    grade_time DATETIME COMMENT '批改时间',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_student_homework (student_id, homework_id),
    INDEX idx_student_id (student_id),
    INDEX idx_homework_id (homework_id),
    INDEX idx_status (status),
    FOREIGN KEY (homework_id) REFERENCES homework(id) ON DELETE CASCADE
);
```

### 4. 学生答题记录表 (student_answers)
```sql
CREATE TABLE student_answers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_homework_id BIGINT NOT NULL COMMENT '学生作业ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    student_answer TEXT COMMENT '学生答案',
    score INT DEFAULT 0 COMMENT '得分',
    is_correct TINYINT DEFAULT 0 COMMENT '是否正确：1-正确，0-错误',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_student_question (student_homework_id, question_id),
    INDEX idx_student_homework_id (student_homework_id),
    INDEX idx_question_id (question_id),
    FOREIGN KEY (student_homework_id) REFERENCES student_homework(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES homework_questions(id) ON DELETE CASCADE
);
```

---

# 学生互动模块数据库设计

## 核心表结构

### 1. 学生问题表 (student_questions)
```sql
CREATE TABLE student_questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL COMMENT '学生ID',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    title VARCHAR(255) NOT NULL COMMENT '问题标题',
    content TEXT NOT NULL COMMENT '问题内容',
    type VARCHAR(20) DEFAULT 'other' COMMENT '问题类型：java,vue,database,frontend,other',
    status TINYINT DEFAULT 0 COMMENT '状态：0-待回答，1-已回答',
    answer TEXT COMMENT '教师回答内容',
    rating TINYINT DEFAULT 0 COMMENT '学生评分：1-5分',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提问时间',
    answered_time DATETIME COMMENT '回答时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_student_id (student_id),
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_status (status),
    INDEX idx_type (type),
    INDEX idx_created_time (created_time),
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE CASCADE
);
```

### 2. 学生-教师关系表 (student_teacher)
```sql
CREATE TABLE student_teacher (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL COMMENT '学生ID',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    status TINYINT DEFAULT 1 COMMENT '状态：1-有效，0-无效',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_student_teacher (student_id, teacher_id),
    INDEX idx_student_id (student_id),
    INDEX idx_teacher_id (teacher_id),
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE CASCADE
);
```

## API接口设计

### 学生端接口

#### 1. 提交问题
```http
POST /student/question/submit
```
**请求参数：**
```json
{
    "studentId": 1,
    "teacherId": 2,
    "title": "Java中继承和接口的区别是什么？",
    "content": "我在学习Java面向对象编程时，对继承和接口的概念有些混淆...",
    "type": "java"
}
```

#### 2. 获取我的问题列表
```http
GET /student/questions/{studentId}
```
**响应数据：**
```json
{
    "code": 1,
    "msg": "success",
    "data": [
        {
            "id": 1,
            "title": "Java中继承和接口的区别是什么？",
            "content": "我在学习Java面向对象编程时...",
            "type": "java",
            "status": 1,
            "createdTime": "2024-01-15 14:30:00",
            "answeredTime": "2024-01-15 16:45:00",
            "answer": "继承是类与类之间的关系...",
            "teacherName": "张老师",
            "rating": 5
        }
    ]
}
```

#### 3. 获取问题详情
```http
GET /student/question/detail/{questionId}
```

#### 4. 评价回答
```http
POST /student/question/rate
```
**请求参数：**
```json
{
    "questionId": 1,
    "rating": 5
}
```

#### 5. 获取互动统计
```http
GET /student/interact/stats/{studentId}
```
**响应数据：**
```json
{
    "code": 1,
    "msg": "success",
    "data": {
        "totalQuestions": 12,
        "answeredQuestions": 8,
        "pendingQuestions": 4,
        "avgRating": 4.2,
        "avgResponseTime": 3.5
    }
}
```

### 教师端接口

#### 1. 获取学生问题列表
```http
GET /teacher/studentQuestion/{teacherId}
```
**响应数据：**
```json
{
    "code": 1,
    "msg": "success",
    "data": [
        {
            "id": 1,
            "studentName": "张三",
            "title": "Java中继承和接口的区别是什么？",
            "content": "我在学习Java面向对象编程时...",
            "type": "java",
            "status": 0,
            "createdTime": "2024-01-15 14:30:00"
        }
    ]
}
```

#### 2. 回答学生问题
```http
POST /teacher/studentAnswer
```
**请求参数：**
```json
{
    "questionId": 1,
    "answer": "继承是类与类之间的关系，表示'是一个'的关系..."
}
```

#### 3. 获取互动统计
```http
GET /teacher/interact/stats/{teacherId}
```

## 业务逻辑说明

### 1. 学生提问流程
1. 学生选择问题类型，填写标题和详细描述
2. 系统根据学生-教师关系自动分配对应的教师
3. 问题状态设为待回答(status=0)
4. 教师在互动模块中可以看到待回答的问题

### 2. 教师回答流程
1. 教师查看学生问题列表，按时间或类型筛选
2. 点击问题查看详情，编写回答内容
3. 提交回答后，问题状态变为已回答(status=1)
4. 记录回答时间，学生可以看到回答

### 3. 学生评价流程
1. 学生查看已回答的问题
2. 对教师回答进行1-5分评价
3. 评价结果用于统计教师回答质量

### 4. 统计计算逻辑
- **总提问数**：学生累计提问次数
- **已回答**：状态为已回答的问题数量
- **待回答**：状态为待回答的问题数量
- **平均评分**：所有评价的平均分
- **平均响应时间**：从提问到回答的平均时间(小时)

## 数据关系图

```
student_questions (学生问题)
    ├── student_id → users(id) [N:1]
    └── teacher_id → users(id) [N:1]

student_teacher (学生-教师关系)
    ├── student_id → users(id) [N:1]
    └── teacher_id → users(id) [N:1]
```

## 索引优化建议

1. **student_questions表**：
   - student_id, teacher_id（复合索引）
   - status, created_time（复合索引）
   - type（单独索引）

2. **student_teacher表**：
   - student_id, teacher_id（唯一复合索引）

## 注意事项

1. **权限控制**：学生只能查看自己的问题，教师只能查看分配给自己的问题
2. **数据完整性**：使用外键约束确保学生-教师关系的有效性
3. **性能优化**：对于统计查询，考虑使用Redis缓存热点数据
4. **通知机制**：新问题提交和回答完成时，可以通过WebSocket推送通知
5. **敏感内容过滤**：对问题内容和回答内容进行敏感词过滤

## API接口设计

### 学生端接口

#### 1. 获取作业列表
```http
GET /student/homework/{studentId}
```
**响应数据：**
```json
{
    "code": 1,
    "msg": "success",
    "data": [
        {
            "id": 1,
            "title": "Java面向对象编程练习",
            "description": "完成Java类与对象的相关练习题",
            "deadline": "2024-01-20 23:59:59",
            "status": "pending",
            "score": 0,
            "totalScore": 100,
            "feedback": "",
            "teacherName": "张老师",
            "createdTime": "2024-01-15 10:00:00"
        }
    ]
}
```

#### 2. 获取作业详情
```http
GET /student/homework/detail/{homeworkId}
```
**响应数据：**
```json
{
    "code": 1,
    "msg": "success",
    "data": {
        "homework": {
            "id": 1,
            "title": "Java面向对象编程练习",
            "description": "完成Java类与对象的相关练习题",
            "deadline": "2024-01-20 23:59:59",
            "totalScore": 100
        },
        "questions": [
            {
                "id": 1,
                "content": "请解释Java中继承的概念",
                "type": "short",
                "score": 20,
                "studentAnswer": "",
                "options": null
            },
            {
                "id": 2,
                "content": "以下哪个是Java的关键字？",
                "type": "choice",
                "score": 10,
                "studentAnswer": "",
                "options": [
                    {"key": "A", "value": "class"},
                    {"key": "B", "value": "Class"},
                    {"key": "C", "value": "CLASS"},
                    {"key": "D", "value": "以上都是"}
                ]
            }
        ]
    }
}
```

#### 3. 保存草稿
```http
POST /student/homework/draft
```
**请求参数：**
```json
{
    "studentId": 1,
    "homeworkId": 1,
    "answers": {
        "1": "继承是面向对象编程的核心概念之一...",
        "2": "A"
    }
}
```

#### 4. 提交作业
```http
POST /student/homework/submit
```
**请求参数：**
```json
{
    "studentId": 1,
    "homeworkId": 1,
    "answers": {
        "1": "继承是面向对象编程的核心概念之一...",
        "2": "A"
    }
}
```

#### 5. 获取作业统计
```http
GET /student/homework/stats/{studentId}
```
**响应数据：**
```json
{
    "code": 1,
    "msg": "success",
    "data": {
        "totalHomework": 12,
        "completedHomework": 8,
        "pendingHomework": 4,
        "averageScore": 85.6,
        "overallProgress": 78
    }
}
```

### 教师端接口

#### 1. 获取作业提交列表
```http
GET /teacher/homework/{homeworkId}/submissions
```

#### 2. 批改作业
```http
POST /teacher/homework/grade
```
**请求参数：**
```json
{
    "studentHomeworkId": 1,
    "scores": {
        "1": 18,
        "2": 10
    },
    "totalScore": 28,
    "feedback": "回答较好，继承概念理解正确..."
}
```

## 业务逻辑说明

### 1. 作业发布流程
1. 教师在练习模块创建作业（题目、分值、截止时间）
2. 系统自动计算总分并保存到homework表
3. 根据学生-教师关系，为相关学生创建student_homework记录（状态为pending）

### 2. 学生答题流程
1. 学生查看作业列表，选择待完成作业
2. 加载作业详情和题目
3. 学生答题过程中可随时保存草稿（更新student_answers表）
4. 提交作业时更新状态为submitted，记录提交时间

### 3. 教师批改流程
1. 教师查看作业提交列表
2. 逐一批改学生答案，给出分数和反馈
3. 更新student_homework状态为graded，记录批改时间

### 4. 统计计算逻辑
- **总作业数**：该学生可见的所有作业数量
- **已完成**：状态为submitted或graded的作业数量
- **待完成**：状态为pending或draft的作业数量
- **平均分**：已批改作业的平均得分
- **整体进度**：已完成作业数 / 总作业数 * 100%

## 数据关系图

```
homework (作业)
    ├── homework_questions (题目) [1:N]
    └── student_homework (学生作业) [1:N]
            └── student_answers (学生答案) [1:N]
```

## 索引优化建议

1. **homework表**：teacher_id, deadline, status
2. **homework_questions表**：homework_id, type
3. **student_homework表**：student_id, homework_id, status（复合索引）
4. **student_answers表**：student_homework_id, question_id（复合索引）

## 注意事项

1. **数据一致性**：使用外键约束确保数据完整性
2. **并发控制**：提交作业时使用乐观锁防止重复提交
3. **性能优化**：对于大量数据的统计查询，考虑使用缓存
4. **数据备份**：重要的作业数据需要定期备份
5. **权限控制**：确保学生只能访问自己的作业数据 