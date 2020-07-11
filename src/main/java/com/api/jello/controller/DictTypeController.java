package com.api.jello.controller;

import com.api.jello.dao.SysDictTypeDao;
import com.api.jello.entity.SysDict;
import com.api.jello.entity.SysDictType;
import com.api.jello.util.ResultUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * DICT_TYPE(SysDictType)表控制层
 *
 * @author nanaouyang
 * @since 2020-03-27 21:45:29
 */
@RestController
@RequestMapping("sysDictType")
public class DictTypeController {


    @Autowired
    private SysDictTypeDao sysDictTypeDao;

    /**
     * 根据code获取字典类型详情
     * @param code
     * @return
     */
    @GetMapping("getDictTypeOne")
    public Object getDictTypeOne(@RequestParam String code) {
        QueryWrapper<SysDictType> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("code", code);
        return ResultUtil.success(sysDictTypeDao.selectOne(queryWrapper));
    }

    /**
     * 字典类型列表
     * @return
     */
    @GetMapping("listDictType")
    public Object listDictType() {
        return ResultUtil.success(sysDictTypeDao.selectList(null));
    }

    /**
     * 新增或修改字典类型
     * @param sysDictType
     * @return
     */
    @PostMapping("saveDictType")
    public Object saveDictType(@RequestBody SysDictType sysDictType) {
        sysDictType.setUpdateTime(null);
        if (null == sysDictType.getId() || null == sysDictTypeDao.selectById(sysDictType.getId())) {
            return ResultUtil.success(sysDictTypeDao.insert(sysDictType));
        } else {
            return ResultUtil.success(sysDictTypeDao.updateById(sysDictType));
        }
    }

    /**
     * 根据code删除一个字典类型
     * @param sysDictType
     * @return
     */
    @DeleteMapping("removeDictType")
    public Object removeDictType(@RequestBody SysDictType sysDictType) {
        return ResultUtil.success(sysDictTypeDao.deleteById(sysDictType.getId()));
    }
}