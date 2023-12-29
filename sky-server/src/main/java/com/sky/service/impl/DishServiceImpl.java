package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetMealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetMealDishMapper setmealDishMapper;
    /**
     * 新增菜品和对应口味
     * @param dishDTO
     * @return
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        //向菜品表加入一条数据
        dishMapper.insert(dish);

        Long id = dish.getId();


        //向口味表加入N条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && !flavors.isEmpty() ){
            flavors.forEach(dishFlavor ->{
                dishFlavor.setDishId(id);
            });
            dishFlavorMapper.insertBatch(flavors);
        }

    }
    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        PageResult pageResult = new PageResult(page.getTotal(),page.getResult());
        return pageResult;

    }

    /**
     * 菜品批量删除
     * @param ids
     * @return
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //判断菜品是否能够删除---是否在起售中
        ids.forEach(id -> {
            Dish dish = dishMapper.getById(id);
            if( Objects.equals(dish.getStatus(), StatusConstant.ENABLE) ){
                //当前菜品起售中,不允许删除
                //抛出不允许删除异常
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        });
        //判断菜品是否能够删除---若菜品被某个套餐关联
        List<Long> setmealIdsByDishIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if(setmealIdsByDishIds != null && !setmealIdsByDishIds.isEmpty()){
            //当前菜品被套餐关联不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        //若可以删除


//        for ( Long id : ids ) {
//
//            //删除菜品表菜品数据
//            dishMapper.deleteById(id);
//
//            //删除关联口味数据
//            dishFlavorMapper.deleteByDishId(id);
//        }
        //优化

        //根据ID批量删除菜品表菜品数据
        dishMapper.deleteByIds(ids);

        //根据菜品ID批量删除关联口味数据
        dishFlavorMapper.deleteByDishIds(ids);



    }

    /**
     * 根据ID查询菜品
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        //根据ID查询菜品数据
        Dish dish = dishMapper.getById(id);


        //根据ID查询菜品关联口味
        List<DishFlavor> dishFlavor = dishFlavorMapper.getByDishId(id);


        //将查询数据封装到VO
        DishVO dishVO = new DishVO();
        dishVO.setFlavors(dishFlavor);
        BeanUtils.copyProperties(dish,dishVO);
        return dishVO;
    }

    /**
     * 修改菜品
     * @param dishDTO
     * @return
     */
    @Override
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);

        //修改菜品基本信息
        dishMapper.update(dish);


        //删除原有口味数据
        dishFlavorMapper.deleteByDishId(dish.getId());

        //重新插入新口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && !flavors.isEmpty() ){
            flavors.forEach(dishFlavor ->{
                dishFlavor.setDishId(dishDTO.getId());
            });
            dishFlavorMapper.insertBatch(flavors);
        }



    }
    /**
     * 根据分类ID获取菜品
     * @param categoryId
     * @return
     */

    @Override
    public List<DishVO> getByCategoryId(Long categoryId) {
        List<DishVO> list = dishMapper.getByCategoryId(categoryId);
        return list;
    }

    /**
     * @param status
     * @param id
     * @return
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(dish);
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<DishVO> dishList = dishMapper.getByCategoryId(dish.getCategoryId());

        List<DishVO> dishVOList = new ArrayList<>();

        for (DishVO d : dishList) {
            if( Objects.equals(d.getStatus(), StatusConstant.DISABLE) ){
                continue;
            }

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            d.setFlavors(flavors);
            dishVOList.add(d);
        }

        return dishVOList;
    }
}
