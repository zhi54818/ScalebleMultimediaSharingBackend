# MongoDB 迁移指南

## 已完成的工作

1. ✅ 更新 `pom.xml`：移除 MyBatis Plus、MySQL、ShardingSphere 依赖，添加 MongoDB 依赖
2. ✅ 更新实体类：将 MyBatis Plus 注解改为 MongoDB 注解（User, Picture, Space, SpaceUser）
3. ✅ 将 Mapper 接口改为 MongoDB Repository 接口
4. ✅ 更新配置文件：移除 MySQL 配置，添加 MongoDB 配置
5. ✅ 删除 MyBatis Plus 配置文件和 Mapper XML 文件
6. ✅ 删除分库分表相关代码

## 需要完成的工作

### 1. 更新 Service 接口

所有 Service 接口需要移除 `extends IService<T>`，并更新方法签名：

**示例：UserService.java**

```java
// 移除
public interface UserService extends IService<User> {
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
}

// 改为
public interface UserService {
    // 移除 getQueryWrapper 方法，或改为返回 Criteria
    // 其他方法保持不变
}
```

### 2. 更新 Service 实现

所有 Service 实现需要：

- 移除 `extends ServiceImpl<Mapper, Entity>`
- 使用 `@Autowired` 或 `@Resource` 注入 Repository
- 将 MyBatis Plus 的查询方法替换为 MongoDB 查询

**关键替换：**

#### QueryWrapper → Criteria

```java
// MyBatis Plus
QueryWrapper<User> queryWrapper = new QueryWrapper<>();
queryWrapper.eq("user_account", userAccount);
User user = userMapper.selectOne(queryWrapper);

// MongoDB
Criteria criteria = Criteria.where("userAccount").is(userAccount);
Query query = new Query(criteria);
User user = mongoTemplate.findOne(query, User.class);
```

#### Page 分页

```java
// MyBatis Plus
Page<User> userPage = userService.page(new Page<>(current, pageSize), queryWrapper);

// MongoDB
Query query = new Query(criteria);
long total = mongoTemplate.count(query, User.class);
query.with(PageRequest.of((int)current - 1, (int)pageSize));
List<User> users = mongoTemplate.find(query, User.class);
MongoPage<User> userPage = new MongoPage<>(users, total, current, pageSize);
```

#### UpdateWrapper → Update

```java
// MyBatis Plus
UpdateWrapper<Picture> updateWrapper = new UpdateWrapper<>();
updateWrapper.eq("id", picture.getId()).eq("space_id", spaceId);
this.update(picture, updateWrapper);

// MongoDB
Query query = new Query();
query.addCriteria(Criteria.where("id").is(picture.getId()));
query.addCriteria(Criteria.where("spaceId").is(spaceId));
Update update = new Update();
// 设置更新字段...
mongoTemplate.updateFirst(query, update, Picture.class);
```

#### Lambda 查询

```java
// MyBatis Plus
this.lambdaQuery()
    .eq(Picture::getSpaceId, spaceId)
    .in(Picture::getId, pictureIdList)
    .list();

// MongoDB
Query query = new Query();
query.addCriteria(Criteria.where("spaceId").is(spaceId));
query.addCriteria(Criteria.where("id").in(pictureIdList));
List<Picture> pictures = mongoTemplate.find(query, Picture.class);
```

### 3. 更新 Controller

所有 Controller 中的 `Page` 需要改为 `MongoPage`：

```java
// 替换
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
// 为
import com.xjzai1.xjzai1picturebackend.common.MongoPage;
```

### 4. 字段名映射

MongoDB 使用驼峰命名，注意：

- `user_account` → `userAccount`
- `user_id` → `userId`
- `space_id` → `spaceId`
- `is_delete` → `isDelete`
- `create_time` → `createTime`
- `update_time` → `updateTime`

### 5. ID 类型转换

所有 `Long` 类型的 ID 已改为 `String` 类型（MongoDB ObjectId），需要更新：

- DTO 中的 ID 字段
- Service 方法中的 ID 参数类型
- Controller 中的 ID 参数类型

### 6. 逻辑删除

MongoDB 没有逻辑删除注解，需要手动处理：

```java
// 查询时添加条件
query.addCriteria(Criteria.where("isDelete").is(0));

// 删除时更新字段
Update update = new Update();
update.set("isDelete", 1);
mongoTemplate.updateFirst(query, update, Entity.class);
```

### 7. 事务处理

MongoDB 4.0+ 支持事务，但需要：

- 使用副本集（Replica Set）
- 配置事务管理器

如果不需要事务，可以移除 `@Transactional` 注解或使用 MongoDB 的事务 API。

### 8. 聚合查询

对于复杂的聚合查询（如 SpaceAnalyzeService），需要使用 MongoDB 的聚合框架：

```java
// 示例：按分类统计
Aggregation aggregation = Aggregation.newAggregation(
    Aggregation.match(criteria),
    Aggregation.group("category")
        .count().as("count")
        .sum("pictureSize").as("totalSize"),
    Aggregation.sort(Sort.Direction.DESC, "count")
);
List<Map> results = mongoTemplate.aggregate(aggregation, "picture", Map.class)
    .getMappedResults();
```

## 需要更新的文件列表

### Service 接口

- `UserService.java`
- `PictureService.java`
- `SpaceService.java`
- `SpaceUserService.java`
- `SpaceAnalyzeService.java`

### Service 实现

- `UserServiceImpl.java`
- `PictureServiceImpl.java`
- `SpaceServiceImpl.java`
- `SpaceUserServiceImpl.java`
- `SpaceAnalyzeServiceImpl.java`

### Controller

- `UserController.java`
- `PictureController.java`
- `SpaceController.java`
- `SpaceUserController.java`
- `SpaceAnalyzeController.java`

### DTO

- 所有包含 `Long id` 的 DTO 需要改为 `String id`

## 注意事项

1. **ID 生成**：MongoDB 自动生成 ObjectId，不需要手动设置
2. **时间字段**：保持 `Date` 类型，MongoDB 支持
3. **索引**：需要在 MongoDB 中手动创建索引
4. **数据迁移**：需要编写脚本将 MySQL 数据迁移到 MongoDB

## 测试建议

1. 先完成一个完整的 Service（如 UserService）进行测试
2. 确保所有 CRUD 操作正常
3. 测试分页功能
4. 测试复杂查询
5. 测试事务（如果使用）

## 数据迁移脚本

需要编写脚本将现有 MySQL 数据迁移到 MongoDB，包括：

- 表结构映射
- 数据类型转换
- ID 转换（Long → String）
- 关系数据迁移
