package com.xjzai1.xjzai1picturebackend.service.impl;

import java.awt.*;
import java.io.IOException;
import java.util.*;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xjzai1.xjzai1picturebackend.common.MongoPage;
import com.xjzai1.xjzai1picturebackend.utils.MongoQueryUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import com.xjzai1.xjzai1picturebackend.config.AzureBlobClientConfig;
import com.xjzai1.xjzai1picturebackend.exception.BusinessException;
import com.xjzai1.xjzai1picturebackend.exception.ErrorCode;
import com.xjzai1.xjzai1picturebackend.exception.ThrowUtils;
import com.xjzai1.xjzai1picturebackend.manager.AzureBlobManager;
import com.xjzai1.xjzai1picturebackend.manager.upload.FilePictureUpload;
import com.xjzai1.xjzai1picturebackend.manager.upload.PictureUploadTemplate;
import com.xjzai1.xjzai1picturebackend.manager.upload.UrlPictureUpload;
import com.xjzai1.xjzai1picturebackend.model.domain.Picture;
import com.xjzai1.xjzai1picturebackend.model.domain.User;
import com.xjzai1.xjzai1picturebackend.model.dto.picture.*;
import com.xjzai1.xjzai1picturebackend.model.vo.PictureVo;
import com.xjzai1.xjzai1picturebackend.model.vo.UserVo;
import com.xjzai1.xjzai1picturebackend.service.PictureService;
import com.xjzai1.xjzai1picturebackend.mapper.PictureMapper;
import com.xjzai1.xjzai1picturebackend.service.UserService;
import com.xjzai1.xjzai1picturebackend.utils.ColorSimilarUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Administrator
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-02-15 17:03:23
 */
@Service
@Slf4j
public class PictureServiceImpl implements PictureService {

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private UserService userService;

    @Resource
    private AzureBlobManager azureBlobManager;

    @Autowired
    private AzureBlobClientConfig azureBlobClientConfig;

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public PictureVo uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH);
        // 用于判断是新增还是更新图片
        String pictureId = pictureUploadRequest.getId();
        // 如果是更新图片，需要校验图片是否存在
        if (StrUtil.isNotBlank(pictureId)) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "Picture not found");
            // 仅本人可修改
            if (!oldPicture.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
            // 并且删除COS中的旧图片
            clearPictureFile(oldPicture);
        }
        // 上传图片，得到信息
        // 按照用户 id 划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        // 根据 inputSource 类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 构造要入库的图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        picture.setOriginalUrl(uploadPictureResult.getOriginalUrl());
        // 图片名称特判，因为爬虫抓取那里可能会传入名称
        String pictureName = uploadPictureResult.getName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPictureName())) {
            pictureName = pictureUploadRequest.getPictureName();
        }
        picture.setName(pictureName);
        picture.setPictureSize(uploadPictureResult.getPictureSize());
        picture.setPictureWidth(uploadPictureResult.getPictureWidth());
        picture.setPictureHeight(uploadPictureResult.getPictureHeight());
        picture.setPictureFormat(uploadPictureResult.getPictureFormat());
        picture.setUserId(loginUser.getId());
        picture.setCreateTime(new Date());
        picture.setUpdateTime(new Date());
        // 如果 pictureId 不为空，表示更新，否则是新增
        if (StrUtil.isNotBlank(pictureId)) {
            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        picture.setIsDelete(0);
        
        // 保存或更新图片
        Picture savedPicture;
        if (StrUtil.isNotBlank(pictureId)) {
            Query query = new Query();
            query.addCriteria(Criteria.where("id").is(pictureId));
            Update update = new Update();
            if (StrUtil.isNotBlank(picture.getUrl())) update.set("url", picture.getUrl());
            if (StrUtil.isNotBlank(picture.getThumbnailUrl())) update.set("thumbnailUrl", picture.getThumbnailUrl());
            if (StrUtil.isNotBlank(picture.getOriginalUrl())) update.set("originalUrl", picture.getOriginalUrl());
            if (StrUtil.isNotBlank(picture.getName())) update.set("name", picture.getName());
            if (picture.getPictureSize() != null) update.set("pictureSize", picture.getPictureSize());
            if (picture.getPictureWidth() != null) update.set("pictureWidth", picture.getPictureWidth());
            if (picture.getPictureHeight() != null) update.set("pictureHeight", picture.getPictureHeight());
            if (StrUtil.isNotBlank(picture.getPictureFormat())) update.set("pictureFormat", picture.getPictureFormat());
            update.set("editTime", new Date());
            mongoTemplate.updateFirst(query, update, Picture.class);
            savedPicture = picture;
        } else {
            savedPicture = pictureMapper.save(picture);
        }
        ThrowUtils.throwIf(savedPicture == null, ErrorCode.OPERATION_ERROR, "Picture upload failed");
        return PictureVo.objToVo(savedPicture);
    }


    @Override
    public MongoPage<Picture> page(PictureQueryRequest pictureQueryRequest) {
        if (pictureQueryRequest == null) {
            return new MongoPage<>();
        }
        Query query = new Query();
        Criteria criteria = new Criteria();
        
        // 逻辑删除过滤
        MongoQueryUtils.addLogicDelete(criteria);
        
        // 取值
        String id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        Long pictureSize = pictureQueryRequest.getPictureSize();
        Integer pictureWidth = pictureQueryRequest.getPictureWidth();
        Integer pictureHeight = pictureQueryRequest.getPictureHeight();
        String pictureFormat = pictureQueryRequest.getPictureFormat();
        String searchText = pictureQueryRequest.getSearchText();
        String userId = pictureQueryRequest.getUserId();
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        
        MongoQueryUtils.addRange(criteria, "editTime", startEditTime, endEditTime);
        
        if (StrUtil.isNotBlank(searchText)) {
            criteria.orOperator(
                Criteria.where("name").regex(searchText, "i"),
                Criteria.where("introduction").regex(searchText, "i")
            );
        }
        
        MongoQueryUtils.addEq(criteria, "id", id);
        MongoQueryUtils.addEq(criteria, "userId", userId);
        MongoQueryUtils.addLike(criteria, "name", name);
        MongoQueryUtils.addLike(criteria, "introduction", introduction);
        MongoQueryUtils.addLike(criteria, "pictureFormat", pictureFormat);
        MongoQueryUtils.addEq(criteria, "category", category);
        MongoQueryUtils.addEq(criteria, "pictureSize", pictureSize);
        MongoQueryUtils.addEq(criteria, "pictureWidth", pictureWidth);
        MongoQueryUtils.addEq(criteria, "pictureHeight", pictureHeight);
        
        query.addCriteria(criteria);
//        MongoQueryUtils.addSort(query, sortField, sortOrder);
        query.with(Sort.by(Sort.Direction.DESC, "_id"));
        // 分页
        long total = mongoTemplate.count(query, Picture.class);
        MongoQueryUtils.addPagination(query, current, pageSize);
        
        List<Picture> pictures = mongoTemplate.find(query, Picture.class);
        return new MongoPage<>(pictures, total, current, pageSize);
    }

    @Override
    public PictureVo getPictureVo(Picture picture, HttpServletRequest request) {
        PictureVo pictureVo = PictureVo.objToVo(picture);
        String userId = pictureVo.getUserId();
        if (StrUtil.isNotBlank(userId)) {
            User user = userService.getById(userId);
            UserVo userVo = userService.getUserVo(user);
            pictureVo.setUser(userVo);
        }
        return pictureVo;
    }

    @Override
    public MongoPage<PictureVo> getPictureVoPage(MongoPage<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        MongoPage<PictureVo> pictureVoPage = new MongoPage<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVoPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVo> pictureVoList = pictureList.stream().map(PictureVo::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<String> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<String, List<User>> userIdUserListMap = userService.listByIds(CollUtil.newArrayList(userIdSet)).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVoList.forEach(pictureVo -> {
            String userId = pictureVo.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVo.setUser(userService.getUserVo(user));
        });
        pictureVoPage.setRecords(pictureVoList);
        return pictureVoPage;
    }

    @Override
    public boolean deletePicture(String pictureId, User loginUser) {
        // 判断是否存在
        Picture oldPicture = this.getById(pictureId);
        if (oldPicture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 校验权限：仅本人可删除
        if (!oldPicture.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        // 操作数据库 - 逻辑删除
        Query query = new Query();
        query.addCriteria(Criteria.where("id").is(pictureId));
        Update update = new Update();
        update.set("isDelete", 1);
        mongoTemplate.updateFirst(query, update, Picture.class);
        // 异步清理文件
        this.clearPictureFile(oldPicture);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void doPictureDeleteByBatch(PictureDeleteByBatchRequest pictureDeleteByBatchRequest, User loginUser) {
        List<String> pictureIdList = pictureDeleteByBatchRequest.getPictureIdList();
        // 1. 校验参数
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH);
        // 2. 查询指定图片
        Query query = new Query();
        query.addCriteria(Criteria.where("id").in(pictureIdList)
                .and("isDelete").is(0));
        List<Picture> pictureList = mongoTemplate.find(query, Picture.class);
        if (pictureList.isEmpty()) {
            return;
        }
        // 3. 校验权限并删除
        for (Picture picture : pictureList) {
            // 仅本人可删除
            if (!picture.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH, "No permission to delete this picture");
            }
            // 逻辑删除
            Query deleteQuery = new Query();
            deleteQuery.addCriteria(Criteria.where("id").is(picture.getId()));
            Update update = new Update();
            update.set("isDelete", 1);
            mongoTemplate.updateFirst(deleteQuery, update, Picture.class);
        }
        // 4. 清理所有文件
        this.clearPictureFiles(pictureList);
    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        // 判断是否存在
        String id = pictureEditRequest.getId();
        // 查询原图片
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限：仅本人可编辑
        if (!oldPicture.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        // 操作数据库
        Query query = new Query();
        query.addCriteria(Criteria.where("id").is(id));
        Update update = new Update();
        if (StrUtil.isNotBlank(picture.getName())) {
            update.set("name", picture.getName());
        }
        if (StrUtil.isNotBlank(picture.getIntroduction())) {
            update.set("introduction", picture.getIntroduction());
        }
        if (StrUtil.isNotBlank(picture.getCategory())) {
            update.set("category", picture.getCategory());
        }
        update.set("editTime", new Date());
        mongoTemplate.updateFirst(query, update, Picture.class);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        List<String> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        String category = pictureEditByBatchRequest.getCategory();
        String nameRule = pictureEditByBatchRequest.getNameRule();
        // 1. 校验参数
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH);
        // 2. 查询指定图片
        Query query = new Query();
        query.addCriteria(Criteria.where("id").in(pictureIdList)
                .and("isDelete").is(0));
        List<Picture> pictureList = mongoTemplate.find(query, Picture.class);
        if (pictureList.isEmpty()) {
            return;
        }
        // 3. 校验权限：仅本人可编辑
        for (Picture picture : pictureList) {
            if (!picture.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH, "No permission to edit this picture");
            }
        }
        // 4. 更新分类和标签
        pictureList.forEach(picture -> {
            if (StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
            }
        });
        // 5. 更新重命名
        this.fillPictureWithNameRule(pictureList, nameRule);
        // 6. 批量更新
        for (Picture picture : pictureList) {
            Query updateQuery = new Query();
            updateQuery.addCriteria(Criteria.where("id").is(picture.getId()));
            Update update = new Update();
            if (StrUtil.isNotBlank(picture.getCategory())) {
                update.set("category", picture.getCategory());
            }
            if (StrUtil.isNotBlank(picture.getName())) {
                update.set("name", picture.getName());
            }
            update.set("editTime", new Date());
            mongoTemplate.updateFirst(updateQuery, update, Picture.class);
        }
    }


    @Override
    public Picture getById(String id) {
        Optional<Picture> pictureOpt = pictureMapper.findById(id);
        if (!pictureOpt.isPresent()) {
            return null;
        }
        Picture picture = pictureOpt.get();
        if (picture.getIsDelete() != null && picture.getIsDelete() == 1) {
            return null;
        }
        return picture;
    }

    @Override
    public List<Picture> listByIds(List<String> ids) {
        if (CollUtil.isEmpty(ids)) {
            return CollUtil.newArrayList();
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("id").in(ids).and("isDelete").is(0));
        return mongoTemplate.find(query, Picture.class);
    }

    @Override
    public boolean save(Picture picture) {
        if (picture.getIsDelete() == null) {
            picture.setIsDelete(0);
        }
        Picture saved = pictureMapper.save(picture);
        return saved != null;
    }

    @Override
    public boolean updateById(Picture picture) {
        if (picture.getId() == null) {
            return false;
        }
        Picture saved = pictureMapper.save(picture);
        return saved != null;
    }

    @Override
    public List<Picture> list(Query query) {
        return mongoTemplate.find(query, Picture.class);
    }

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        if (StrUtil.isNotBlank(id)) {
            if (StrUtil.isNotBlank(url)) {
                ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "URL is too long");
            }
            if (StrUtil.isNotBlank(introduction)) {
                ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "Introduction is too long");
            }
        }
    }


    /**
     * 清理Azure Blob Storage中的图片资源
     *
     * @param oldPicture
     */
    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断该图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        Query query = new Query();
        query.addCriteria(Criteria.where("url").is(pictureUrl)
                .and("isDelete").is(0));
        long count = mongoTemplate.count(query, Picture.class);
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        // 注意，这里的 url 包含了域名，实际上只要传 key 值（存储路径）就够了
        String host = azureBlobClientConfig.getHost();
        // 从 URL 中提取 blob name（移除域名和开头的 /）
        String urlKey = pictureUrl.replace(host, "").replaceFirst("^/+", "");
        azureBlobManager.deleteObject(urlKey);
        // 清理缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            String thumbnailKey = thumbnailUrl.replace(host, "").replaceFirst("^/+", "");
            azureBlobManager.deleteObject(thumbnailKey);
        }
        // 清理原图
        String originalUrl = oldPicture.getOriginalUrl();
        if (StrUtil.isNotBlank(originalUrl)) {
            String originalKey = originalUrl.replace(host, "").replaceFirst("^/+", "");
            azureBlobManager.deleteObject(originalKey);
        }
    }

    /**
     * 批量清理Azure Blob Storage中的图片资源
     *
     * @param pictureList
     */
    @Override
    public void clearPictureFiles(List<Picture> pictureList) {
        List<String> keyList = new ArrayList<>();
        for (Picture oldPicture : pictureList) {
            // 判断该图片是否被多条记录使用
            String pictureUrl = oldPicture.getUrl();
            Query query = new Query();
            query.addCriteria(Criteria.where("url").is(pictureUrl)
                    .and("isDelete").is(0));
            long count = mongoTemplate.count(query, Picture.class);
            // 有不止一条记录用到了该图片，不清理
            if (count > 1) {
                continue;
            }
            // 注意，这里的 url 包含了域名，实际上只要传 key 值（存储路径）就够了
            String host = azureBlobClientConfig.getHost();
            // 从 URL 中提取 blob name（移除域名和开头的 /）
            String urlKey = pictureUrl.replace(host, "").replaceFirst("^/+", "");
            keyList.add(urlKey);
            // 清理缩略图
            String thumbnailUrl = oldPicture.getThumbnailUrl();
            if (StrUtil.isNotBlank(thumbnailUrl)) {
                String thumbnailKey = thumbnailUrl.replace(host, "").replaceFirst("^/+", "");
                keyList.add(thumbnailKey);
            }
            // 清理原图
            String originalUrl = oldPicture.getOriginalUrl();
            if (StrUtil.isNotBlank(originalUrl)) {
                String originalKey = originalUrl.replace(host, "").replaceFirst("^/+", "");
                keyList.add(originalKey);
            }
        }
        azureBlobManager.deleteObjects(keyList);
    }

    /**
     * nameRule 格式：图片{序号}
     *
     * @param pictureList
     * @param nameRule
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (CollUtil.isEmpty(pictureList) || StrUtil.isBlank(nameRule)) {
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(pictureName);
            }
        } catch (Exception e) {
            log.error("Name parsing error", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Name parsing error");
        }
    }

}




