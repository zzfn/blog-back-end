package com.zzf.mapper;

import com.zzf.entity.UserRole;
import com.zzf.entity.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * USER(TUser)表数据库访问层
 *
 * @author nanaouyang
 * @since 2020-04-11 22:40:47
 */
@Repository
public interface UserRoleDao extends BaseMapper<UserRole> {
    List<Role> getRoles(@Param("userId") String userId);

}