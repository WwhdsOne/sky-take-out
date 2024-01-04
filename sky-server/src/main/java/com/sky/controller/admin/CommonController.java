package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * 通用Controller
 */
@RestController
@RequestMapping("/user/common")
@Slf4j
@Api
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;
    /**
     * 文件上传
     * @param file
     * @return
     */
    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(MultipartFile file) {
        log.info("文件上传:{}",file);

        try {
            //获取原始文件名
            String originalFilename = file.getOriginalFilename();
            //获取原始文件名后缀
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String ObjectName = UUID.randomUUID() + extension;
            System.out.println(ObjectName);
            String filepath = aliOssUtil.upload(file.getBytes(), ObjectName);
            log.info("文件上传成功");
            return Result.success(filepath);
        } catch (IOException e) {
            log.info("文件上传失败:{}",e);
        }
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }
}
