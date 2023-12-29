package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetMealDishMapper;
import com.sky.mapper.SetMealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetMealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class SetMealServiceImpl implements SetMealService {

    @Autowired
    SetMealMapper setMealMapper;

    @Autowired
    SetMealDishMapper setMealDishMapper;

    @Autowired
    DishMapper dishMapper;


    /**
     * 新增菜品
     * @param setmealDTO
     * @return
     */
    @Override
    @Transactional
    public void saveSetMealWithDishes(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);

        //插入套餐
        setMealMapper.insert(setmeal);

        //获取分类中的菜品
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();

        Long id = setmeal.getId();
        //批量插入套餐菜品
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(id);
        });

        setMealDishMapper.insertBatch(setmealDishes);



    }

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return PageResult
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setMealMapper.pageQuery(setmealPageQueryDTO);
        PageResult pageResult = new PageResult(page.getTotal(),page.getResult());
        return pageResult;
    }

    /**
     * 根据ID批量删除套餐
     * @param ids
     * @return
     */
    @Override
    @Transactional
    public void deleteBySetMealIds(List<Long> ids) {
        //根据ID批量删除套餐
        ids.forEach(id ->{
            Setmeal setmeal = setMealMapper.getById(id);
            if( Objects.equals(setmeal.getStatus(), StatusConstant.ENABLE) ){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        });
        //批量删除套餐菜品
        setMealDishMapper.deleteBySetMealIds(ids);
        //批量删除套餐
        setMealMapper.deleteByIds(ids);
    }

    /**
     * 根据id查询套餐及套餐菜品
     * @param id
     * @return
     */
    @Override
    public SetmealVO getByIdWithDish(Long id) {
        //新建VO实体对象
        SetmealVO setmealVO = new SetmealVO();
        Setmeal setmeal = setMealMapper.getById(id);
        BeanUtils.copyProperties(setmeal,setmealVO);
        //获取SetmealDish实体对象集合
        List<SetmealDish> list = setMealDishMapper.getSetmealDishBySetmealId(id);
        setmealVO.setSetmealDishes(list);
        return setmealVO;
    }

    /**
     * 修改套餐
     * @param setmealDTO
     * @return
     */
    @Override
    @Transactional
    public void updateWithSetMealDish(SetmealDTO setmealDTO) {

        //创建setmeal实体
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        Integer status = setMealMapper.getById(setmeal.getId()).getStatus();
        if( Objects.equals(status, StatusConstant.ENABLE) ){
            throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
        }

        //修改套餐
        setMealMapper.update(setmeal);

        Long id = setmealDTO.getId();
        //首先根据ID删除原套餐菜品
        setMealDishMapper.deleteBySetMealId(id);
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(id);
        });
        //重新插入与套餐相关的菜品
        setMealDishMapper.insertBatch(setmealDishes);

    }
    /**
     * 根据ID起售停售套餐
     * @param status
     * @param id
     * @return
     */
    @Override
    public void startOrstop(Integer status, Long id) {
        //起售套餐时判断所有套餐菜品是否售卖
        //若有停售菜品则输出"套餐内包含未启售菜品，无法启售"
        if( Objects.equals(status, StatusConstant.ENABLE) ){
            List<Dish> dishes = dishMapper.getBySetMealId(id);
            dishes.forEach(dish -> {
                if( Objects.equals(dish.getStatus(), StatusConstant.DISABLE) ){
                    throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                }
            });
        }
        Setmeal setmeal = Setmeal.builder()
                .status(status)
                .id(id)
                .build();
        setMealMapper.update(setmeal);
    }

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setMealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setMealMapper.getDishItemBySetmealId(id);
    }
}
