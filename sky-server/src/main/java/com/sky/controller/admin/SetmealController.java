package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetMealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/setmeal")
@Api
@Slf4j
public class SetmealController {

    @Autowired
    SetMealService setMealService;


    /**
     * 新增菜品
     * @param setmealDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增套餐")
    @CacheEvict(cacheNames = "setmeal_cache",key = "#setmealDTO.categoryId")
    public Result getById(@RequestBody SetmealDTO setmealDTO){
        log.info("新增套餐:{}",setmealDTO);
        setMealService.saveSetMealWithDishes(setmealDTO);
        return Result.success();
    }

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("套餐分页查询")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO){
        log.info("套餐分页查询:{}",setmealPageQueryDTO);
        PageResult page = setMealService.pageQuery(setmealPageQueryDTO);
        return Result.success(page);
    }

    /**
     * 根据ID批量删除套餐
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("根据ID批量删除套餐")
    @CacheEvict(cacheNames = "setmeal_cache",allEntries = true)
    public Result delete(@RequestParam List<Long> ids){
        log.info("根据ID批量删除套餐:{}",ids);
        setMealService.deleteBySetMealIds(ids);
        return Result.success();
    }

    /**
     * 根据id查询套餐及套餐菜品
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询套餐")
    public Result<SetmealVO> getById(@PathVariable Long id){
        log.info("根据ID查询套餐:{}",id);
        SetmealVO setmealVO = setMealService.getByIdWithDish(id);
        return Result.success(setmealVO);
    }

    /**
     * 修改套餐
     * @param setmealDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改套餐")
    @CacheEvict(cacheNames = "setmeal_cache",allEntries = true)
    public Result update(@RequestBody SetmealDTO setmealDTO){
        setMealService.updateWithSetMealDish(setmealDTO);
        return Result.success();
    }

    /**
     * 根据ID起售停售套餐
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("根据ID起售停售套餐")
    @CacheEvict(cacheNames = "setmeal_cache",allEntries = true)
    public Result startOrStop(@PathVariable Integer status,Long id){
        setMealService.startOrstop(status,id);
        return Result.success();
    }
}
