package com.zzf.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzf.component.IgnoreAuth;
import com.zzf.component.Send;
import com.zzf.entity.Article;
import com.zzf.entity.ArticleEs;
import com.zzf.mapper.ArticleESDao;
import com.zzf.util.HttpUtil;
import com.zzf.util.ResultUtil;
import com.zzf.vo.ArticleVO;
import com.zzf.vo.PageVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import com.zzf.mapper.ArticleDao;
import com.zzf.service.ArticleService;
import com.zzf.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * @author zzf
 */
@RestController
@RequestMapping("article")
@Slf4j
@Api(tags = "文章管理")
public class ArticleController {
    static final String TAG_DESC = "tagDesc";
    static final String TITLE = "title";
    static final String CONTENT = "content";

    @Autowired
    ArticleDao articleMapper;
    @Autowired
    ArticleService articleService;
    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;
    @Autowired
    RedisUtil redisUtil;
    @Autowired
    private Send send;
    @GetMapping("page")
    @ApiOperation("文章分页列表")
    @IgnoreAuth
    public Object pageArticles(PageVO pageVO) {
        QueryWrapper<Article> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq( "IS_RELEASE", 1).orderByDesc("ORDER_NUM").orderByDesc("CREATE_TIME");
        IPage<Article> page = new Page<>(pageVO.getCurrent(), pageVO.getPageSize());
        IPage<Article> pageList = articleMapper.selectPage(page, queryWrapper);
        return ResultUtil.success(pageList);
    }

    //后台
    @PostMapping("admin/save")
    @ApiOperation("保存或修改文章")
    @PreAuthorize("hasRole('ADMIN')")
    public Object saveArticle(@RequestBody Article article) {
        articleService.saveOrUpdate(article);
        send.post(article);
        return ResultUtil.success(article.getId());
    }



    @ApiOperation("文章分页列表")
    @GetMapping("non/page")
    public Object listArticles(ArticleVO articleVO) {
        HashMap<String, String> map = new HashMap<>();
        map.put("updateTime", "UPDATE_TIME");
        map.put("createTime", "CREATE_TIME");
        QueryWrapper<Article> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(null != articleVO.getTag(), "TAG", articleVO.getTag()).eq(null != articleVO.getIsRelease(), "IS_RELEASE", articleVO.getIsRelease()).like(StringUtils.isNoneEmpty(articleVO.getTitle()), "TITLE", articleVO.getTitle()).eq(articleVO.getIsOnlyRelease(), "IS_RELEASE", 1).orderBy(null != articleVO.getField(), "ascend".equals(articleVO.getOrder()), map.get(articleVO.getField())).orderByDesc("ORDER_NUM").orderByDesc("CREATE_TIME");
        IPage<Article> page = new Page<>(articleVO.getCurrent(), articleVO.getPageSize());
        IPage<Article> pageList = articleMapper.selectPage(page, queryWrapper);
        return ResultUtil.success(pageList);
    }

    @ApiOperation("排行榜")
    @GetMapping("non/hot")
    public Object listHotArticles() {
        Collection ids = new ArrayList();
        List<Double> num = new ArrayList();
        Set<ZSetOperations.TypedTuple<Object>> typedTuple1 = redisUtil.reverseRangeWithScores("viewCount", 0L, 9L);
        typedTuple1.forEach(objectTypedTuple -> {
            ids.add(objectTypedTuple.getValue());
            num.add(objectTypedTuple.getScore());
        });
        List<Article> articles = articleService.listByIds(ids);
        for (int i = 0; i < articles.size(); i++) {
            articles.get(i).setViewCount(num.get(i).longValue());
        }
        return ResultUtil.success(articles);
    }

    @ApiOperation("文章分类")
    @GetMapping("/non/tags")
    public Object listTags() {
        return ResultUtil.success(articleMapper.getTags());
    }

    @ApiOperation("文章列表不分页")
    @GetMapping("non/list")
    public Object listArchives(@RequestParam(defaultValue = "") String code) {
        return ResultUtil.success(articleService.listByCache(code));
    }

    @ApiOperation("最近更新")
    @GetMapping("non/lastUpdated")
    public Object lastUpdated() {
        return ResultUtil.success(articleMapper.listLastUpdated());
    }

    @ApiOperation("更新浏览量")
    @GetMapping("non/updateViewed")
    public Object updateViewed(@RequestParam String id) {
        String isViewed = "isViewed::" + HttpUtil.getIp() + "::" + id;
        if (redisUtil.hasKey(isViewed)) {
            return ResultUtil.success(false);
        } else {
            redisUtil.set(isViewed, 1, 3600L);
            redisUtil.incZSetValue("viewCount", id, 1L);
            return ResultUtil.success(true);
        }
    }

    @ApiOperation("点赞")
    @PostMapping("non/star")
    public Object star(@RequestParam String id) {
        String isStared = "isStared::" + HttpUtil.getIp() + "::" + id;
        if (redisUtil.hasKey(isStared)) {
            return ResultUtil.success(false);
        } else {
            redisUtil.set(isStared, 1);
            redisUtil.incZSetValue("starCount", id, 1L);
            return ResultUtil.success(true);
        }
    }

    @ApiOperation("根据id查询文章详情-前台")
    @GetMapping("non/{id}")
    public Object getArticle(@PathVariable("id") String id) {
        if (StringUtils.isBlank(id)) {
            return ResultUtil.error("请传文章id");
        }
        Article article = articleService.getByCache(id);
        if (null == article || !article.getIsRelease()) {
            return ResultUtil.error("文章已下线");
        }
        Double num = redisUtil.zScore("viewCount", id);
        if (null == num) {
            article.setViewCount(1L);
        } else {
            article.setViewCount(num.longValue());
        }
        return ResultUtil.success(article);
    }

    @ApiOperation("根据id查询文章详情-后台")
    @GetMapping("{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Object getArticleAdmin(@PathVariable("id") String id) {
        Article article = articleService.getByDb(id);
        return ResultUtil.success(article);
    }

    @ApiOperation("根据id删除文章")
    @DeleteMapping("non/{id}/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public Object removeArticle(@PathVariable String id, @PathVariable String code) {
        articleService.listByDb("");
        articleService.listByDb(code);
        return ResultUtil.success(articleService.delByDb(id));
    }

    @ApiOperation("es搜索")
    @GetMapping("non/search")
    public Object getList(String keyword) {
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        queryBuilder.should(QueryBuilders.matchPhraseQuery(TITLE, keyword))
                .should(QueryBuilders.matchPhraseQuery(CONTENT, keyword))
                .should(QueryBuilders.matchPhraseQuery("tag_desc", keyword));
        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder().withQuery(queryBuilder).withHighlightBuilder(new HighlightBuilder().field(TITLE).field(CONTENT).field("tag_desc")).build();
        List<ArticleEs> list = new ArrayList<>();
        SearchHits<ArticleEs> esSearchHits = elasticsearchRestTemplate.search(nativeSearchQuery, ArticleEs.class);
        esSearchHits.getSearchHits().forEach(searchHit -> {
            ArticleEs articleEs = searchHit.getContent();
            articleEs.setContent(StringUtils.join(searchHit.getHighlightField(CONTENT), " "));
            if (searchHit.getHighlightField(TAG_DESC).size() != 0) {
                articleEs.setTagDesc(StringUtils.join(searchHit.getHighlightField(TAG_DESC), " "));
            }
            if (searchHit.getHighlightField(TITLE).size() != 0) {
                articleEs.setTitle(StringUtils.join(searchHit.getHighlightField(TITLE), " "));
            }
            if (articleEs.getIsRelease() == 1 && articleEs.getIsDelete() == 0) {
                list.add(articleEs);
            }
        });
        return ResultUtil.success(list);
    }

    @GetMapping("es/non/reset")
    public Object reset() {
        elasticsearchRestTemplate.createIndex(ArticleEs.class);
        elasticsearchRestTemplate.putMapping(ArticleEs.class);
        return null;
    }

    @GetMapping("es/non/del")
    public Object del() {
        elasticsearchRestTemplate.deleteIndex(ArticleEs.class);
        return null;
    }
//    @GetMapping("es/reset")
//    @PreAuthorize("hasRole('ADMIN')")
//    public Object reset() {
//        articleESDao.deleteAll();
//        return null;
//    }

}
