package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetMealDishMapper {

    /**
     * 根据菜品ID查询套餐ID
     * @param dishIds
     * @return List&lt;Long&gt;
     */
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);


    /**
     * 新增套餐菜品
     * @param setmealDishes
     * @return
     */
    void insertBatch(List<SetmealDish> setmealDishes);

    /**
     * 根据ID批量删除套餐菜品
     * @param setmealids
     * @return
     */
    void deleteBySetMealIds(List<Long> setmealids);

    /**
     * 根据ID删除套餐
     * @param setmealId
     * @return
     */
    @Delete("delete from setmeal_dish where setmeal_id = #{setmealId}")
    void deleteBySetMealId(Long setmealId);

    /**
     * 根据套餐ID获取套餐菜品
     * @param id
     * @return
     */
    @Select("select * from setmeal_dish where setmeal_id = #{id}")
    List<SetmealDish> getSetmealDishBySetmealId(Long id);
}
