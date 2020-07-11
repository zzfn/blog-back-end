package com.api.jello.controller;

import com.api.jello.dao.ArticleDao;
import com.api.jello.dao.CommentDao;
import com.api.jello.entity.Article;
import com.api.jello.util.ResultUtil;
import com.api.jello.vo.PageVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author cc
 * @date 2020/3/22 17:22
 */
@RestController
@RequestMapping("article")
@Slf4j
public class ArticleController {
    @Autowired
    ArticleDao articleDao;
    @Autowired
    CommentDao commentDao;
    @PostMapping("saveArticle")
    public Object saveArticle(@RequestBody Article article) {
        article.setUpdateTime(null);
        if(null==article.getId()||null == articleDao.selectById(article.getId())){
            article.setViewCount(0);
            return ResultUtil.success(articleDao.insert(article));
        }else {
            return ResultUtil.success(articleDao.updateById(article));
        }
    }

    @GetMapping("listArticles")
    public Object listArticles(PageVO pageVo) {
        IPage<Article> page = new Page<>(pageVo.getPageNumber(), pageVo.getPageSize());
        IPage<Article> pageList = articleDao.listArticle(page);
        return ResultUtil.success(pageList);
    }

    @GetMapping("getArticle")
    public Object getArticle(String id) {
        return ResultUtil.success(articleDao.selectById(id));
    }

    @DeleteMapping("removeArticle")
    public Object removeArticle(@RequestBody Article article) {
        return ResultUtil.success(articleDao.deleteById(article.getId()));
    }
}
