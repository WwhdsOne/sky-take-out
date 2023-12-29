package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishMapper {



    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return Integer
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * 新增菜品
     * @param dish
     * @return
     */
    @AutoFill(value = OperationType.INSERT)
    void insert(Dish dish);

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return Page&lt;DishVo%gt;
     */
    Page<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 根据主键ID查询菜品
     * @param id
     * @return Dish
     */
    @Select("select * from dish where id = #{id}")
    Dish getById(Long id);

    /**
     * 根据主键ID批量删除菜品
     * @param ids
     * @return
     */

    void deleteByIds(List<Long> ids);

    /**
     * 修改菜品
     * @param dish
     * @return
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(Dish dish);

    /**
     * 根据分类ID获取菜品
     * @param categoryId
     * @return
     */
    List<DishVO> getByCategoryId(Long categoryId);


    /**
     * 根据套餐ID获取独立菜品
     * @param SetMealId
     * @return
     */
    @Select("select d.* from dish d left join setmeal_dish s on d.id = s.dish_id where s.setmeal_id = #{SetMealId}")
    List<Dish> getBySetMealId(Long SetMealId);

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */

    @Select("select * from dish where id = #{dish.id}")
    List<Dish> list(Dish dish);
}
