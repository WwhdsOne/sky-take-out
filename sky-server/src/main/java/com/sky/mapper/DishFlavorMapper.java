package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.DishFlavor;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorMapper {

    /**
     * 新增口味
     * @param list
     * @return
     */

    void insertBatch(List<DishFlavor> list);

    /**
     * 根据菜品ID批量删除关联口味
     * @param dishIds
     * @return
     */
    void deleteByDishIds(List<Long> dishIds);

    /**
     * 根据菜品ID删除关联口味
     * @param dishId
     * @return
     */
    @Delete("delete from dish_flavor where dish_id = #{dishId}")
    void deleteByDishId(Long dishId);

    /**
     * 根据菜品ID查询关联口味
     * @param dishId
     * @return List&lt;DishFlavor&gt;
     */
    @Select("select * from dish_flavor where dish_id = #{dishId}")
    List<DishFlavor> getByDishId(Long dishId);
}
