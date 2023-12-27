package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetMealService {

    void saveSetMealWithDishes(SetmealDTO setmealDTO);

    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    void deleteBySetMealIds(List<Long> ids);

    SetmealVO getByIdWithDish(Long id);

    void updateWithSetMealDish(SetmealDTO setmealDTO);

    void startOrstop(Integer status, Long id);
}
